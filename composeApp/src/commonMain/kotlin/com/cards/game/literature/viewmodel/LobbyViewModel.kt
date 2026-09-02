package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.cards.game.literature.analytics.Analytics
import com.cards.game.literature.analytics.AnalyticsEvent
import com.cards.game.literature.repository.ConnectionState
import com.cards.game.literature.repository.OnlineGameRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class LoadingOperation { CREATE, JOIN }

// Give up waiting for room admission after this long. Without it, a graceful close before
// admission (e.g. hitting the per-IP connection rate limit on a crowded Wi-Fi) emits neither an
// error nor a room state, so the lobby spinner would spin forever.
private const val ADMISSION_TIMEOUT_MS = 20_000L

data class LobbyUiState(
    val loadingOperation: LoadingOperation? = null,
    val errorMessage: String? = null,
    val isServerReady: Boolean = false
) {
    val isLoading get() = loadingOperation != null
    // Only show warming-up while actively waiting to connect
    val showWarmingUp get() = isLoading && !isServerReady
}

class LobbyViewModel(
    private val onlineRepository: OnlineGameRepository
) : ViewModel() {

    private val log = Logger.withTag("LobbyViewModel")

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = onlineRepository.connectionState

    private val _navigateToWaitingRoom = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToWaitingRoom: Flow<String> = _navigateToWaitingRoom.asSharedFlow()

    // Completes (even on warmUp failure) so createRoom/joinRoom know when to proceed
    private val serverReadyDeferred = CompletableDeferred<Unit>()

    init {
        Analytics.log(AnalyticsEvent.LobbyOpened)

        viewModelScope.launch {
            onlineRepository.warmUp()
            serverReadyDeferred.complete(Unit)
            _uiState.update { it.copy(isServerReady = true) }
        }

        viewModelScope.launch {
            onlineRepository.errors.collect { error ->
                _uiState.update { it.copy(errorMessage = error, loadingOperation = null) }
            }
        }

        viewModelScope.launch {
            onlineRepository.roomState.filterNotNull().first()
            _uiState.update { it.copy(loadingOperation = null) }
            _navigateToWaitingRoom.emit(onlineRepository.roomCode)
        }
    }

    fun createRoom(playerName: String, playerCount: Int) {
        viewModelScope.launch {
            log.i { "Creating room: player=$playerName, count=$playerCount" }
            Analytics.log(AnalyticsEvent.RoomCreated(playerCount))
            _uiState.update { it.copy(loadingOperation = LoadingOperation.CREATE, errorMessage = null) }
            serverReadyDeferred.await() // waits only if warmUp is still in progress
            onlineRepository.createRoom(playerName, playerCount)
            guardAdmissionTimeout()
        }
    }

    fun joinRoom(roomCode: String, playerName: String) {
        viewModelScope.launch {
            log.i { "Joining room: code=$roomCode, player=$playerName" }
            _uiState.update { it.copy(loadingOperation = LoadingOperation.JOIN, errorMessage = null) }
            serverReadyDeferred.await()
            onlineRepository.joinRoom(roomCode, playerName)
            guardAdmissionTimeout()
        }
    }

    /**
     * Fails the pending create/join if the server never admits us into a room. Only acts while
     * still loading, so a room state (navigates away) or an error (shown normally) that arrives
     * first wins the race and this becomes a no-op — it can't disconnect a live session.
     */
    private fun guardAdmissionTimeout() {
        viewModelScope.launch {
            val admitted = withTimeoutOrNull(ADMISSION_TIMEOUT_MS) {
                onlineRepository.roomState.filterNotNull().first()
            }
            if (admitted == null && _uiState.value.loadingOperation != null) {
                _uiState.update {
                    it.copy(
                        loadingOperation = null,
                        errorMessage = "Couldn't reach the room. Please check your connection and try again."
                    )
                }
                onlineRepository.disconnect()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        if (onlineRepository.connectionState.value == ConnectionState.CONNECTING) {
            onlineRepository.disconnect()
        }
    }
}
