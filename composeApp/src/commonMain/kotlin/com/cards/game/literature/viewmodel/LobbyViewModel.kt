package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.cards.game.literature.analytics.Analytics
import com.cards.game.literature.analytics.AnalyticsEvent
import com.cards.game.literature.repository.ConnectionState
import com.cards.game.literature.repository.OnlineGameRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.cards.game.literature.repository.FatalSessionError

enum class LoadingOperation { CREATE, JOIN, QUICK_PLAY }

// Give up waiting for room admission after this long. Without it, a graceful close before
// admission (e.g. hitting the per-IP connection rate limit on a crowded Wi-Fi) emits neither an
// error nor a room state, so the lobby spinner would spin forever.
private const val ADMISSION_TIMEOUT_MS = 20_000L

data class LobbyUiState(
    val loadingOperation: LoadingOperation? = null,
    val errorMessage: String? = null,
    /** The server never admitted us within the timeout; the screen owns the wording. */
    val admissionTimedOut: Boolean = false,
    /** A terminal session error hit during create/join (e.g. this build is too old). */
    val fatalError: FatalSessionError? = null,
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

    // The request currently waiting to be seated. One at a time: a second tap supersedes the
    // first, so a late admission can never navigate twice.
    private var admission: Job? = null

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

        // The version gate (close 1003) lands on the FIRST connect, i.e. here in the lobby — the
        // only screen that never watched fatalError, so the spinner ran to the timeout and the
        // player got a connectivity message instead of "please update".
        viewModelScope.launch {
            onlineRepository.fatalError.collect { fatal ->
                if (fatal != null) _uiState.update { it.copy(loadingOperation = null, fatalError = fatal) }
            }
        }
    }

    // How the pending join was initiated, reported on admission (see AnalyticsEvent.RoomJoined).
    private var pendingJoinSource = "code"

    fun quickPlay(playerName: String) = seat(LoadingOperation.QUICK_PLAY) {
        log.i { "Quick Play: player=$playerName" }
        onlineRepository.quickPlay(playerName)
    }

    fun createRoom(playerName: String, playerCount: Int, isPublic: Boolean = false) = seat(LoadingOperation.CREATE) {
        log.i { "Creating room: player=$playerName, count=$playerCount, public=$isPublic" }
        Analytics.log(AnalyticsEvent.RoomCreated(playerCount, isPublic))
        onlineRepository.createRoom(playerName, playerCount, isPublic)
    }

    /** [source] is "code" (typed) or "invite" (deep link / referrer auto-join), for analytics. */
    fun joinRoom(roomCode: String, playerName: String, source: String = "code") {
        pendingJoinSource = source
        seat(LoadingOperation.JOIN) {
            log.i { "Joining room: code=$roomCode, player=$playerName" }
            onlineRepository.joinRoom(roomCode, playerName)
        }
    }

    /**
     * Sends one request and waits for ITS admission: the room state the server sends once we
     * hold a seat. Waiting per request, not once per screen, is the point. A single listener
     * armed at start-up was spent by whatever room state was still lying around from the table
     * the player had just left, and the next Quick Play then spun forever on a seat it had in
     * fact been given. The repository forgets the old table before it connects, so the state
     * seen here is always the new room's.
     */
    private fun seat(operation: LoadingOperation, request: suspend () -> Unit) {
        admission?.cancel()
        admission = viewModelScope.launch {
            _uiState.update { it.copy(loadingOperation = operation, errorMessage = null, admissionTimedOut = false) }
            serverReadyDeferred.await() // waits only if warmUp is still in progress
            request()
            val room = withTimeoutOrNull(ADMISSION_TIMEOUT_MS) {
                onlineRepository.roomState.filterNotNull().first()
            }
            if (room == null) {
                // An error or a fatal has already stopped the spinner; otherwise a graceful close
                // before admission emitted neither, and only this stops it from running on.
                if (_uiState.value.loadingOperation != null) {
                    _uiState.update { it.copy(loadingOperation = null, admissionTimedOut = true) }
                    onlineRepository.disconnect()
                }
                return@launch
            }
            when (operation) {
                // A join into a running table delivers the board before the room state.
                LoadingOperation.JOIN -> Analytics.log(AnalyticsEvent.RoomJoined(
                    pendingJoinSource, room.targetPlayerCount, inProgress = onlineRepository.gameState.value != null
                ))
                LoadingOperation.QUICK_PLAY -> Analytics.log(AnalyticsEvent.QuickPlay(
                    outcome = if (room.hostPlayerId == onlineRepository.myPlayerId) "created" else "joined"
                ))
                LoadingOperation.CREATE -> {}
            }
            _uiState.update { it.copy(loadingOperation = null) }
            _navigateToWaitingRoom.emit(onlineRepository.roomCode)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, admissionTimedOut = false) }
    }

    fun clearFatalError() {
        _uiState.update { it.copy(fatalError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        if (onlineRepository.connectionState.value == ConnectionState.CONNECTING) {
            onlineRepository.disconnect()
        }
    }
}
