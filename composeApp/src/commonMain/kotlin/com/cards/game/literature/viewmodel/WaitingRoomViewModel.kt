package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.protocol.RoomPhase
import com.cards.game.literature.protocol.RoomState
import com.cards.game.literature.repository.ConnectionState
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.repository.PlayerConnectionEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WaitingRoomUiState(
    val roomCode: String = "",
    val players: List<WaitingRoomPlayer> = emptyList(),
    val isHost: Boolean = false,
    val myPlayerId: String = "",
    val targetPlayerCount: Int = 6,
    val errorMessage: String? = null,
    val isStarting: Boolean = false,
    val isStartGameTimedOut: Boolean = false
)

data class WaitingRoomPlayer(
    val id: String,
    val name: String,
    val teamId: String,
    val isHost: Boolean,
    val isConnected: Boolean
)

class WaitingRoomViewModel(
    private val onlineRepository: OnlineGameRepository
) : ViewModel() {

    private val log = Logger.withTag("WaitingRoomVM")

    private val _uiState = MutableStateFlow(WaitingRoomUiState())
    val uiState: StateFlow<WaitingRoomUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = onlineRepository.connectionState

    private val _navigateToGame = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToGame: Flow<Unit> = _navigateToGame.asSharedFlow()

    private val _snackbarEvents = MutableSharedFlow<PlayerConnectionEvent>(extraBufferCapacity = 16)
    val snackbarEvents: Flow<PlayerConnectionEvent> = _snackbarEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            onlineRepository.roomState.filterNotNull().collect { room ->
                updateFromRoom(room)
            }
        }

        viewModelScope.launch {
            onlineRepository.gameState.filterNotNull().first()
            _navigateToGame.emit(Unit)
        }

        viewModelScope.launch {
            onlineRepository.errors.collect { error ->
                _uiState.update { it.copy(errorMessage = error, isStarting = false) }
            }
        }

        viewModelScope.launch {
            onlineRepository.playerEvents.collect { event ->
                _snackbarEvents.emit(event)
            }
        }
    }

    fun startGame(fillWithBots: Boolean = true, difficulty: BotDifficulty = BotDifficulty.MEDIUM) {
        viewModelScope.launch {
            log.i { "Starting game, fillWithBots=$fillWithBots, difficulty=$difficulty" }
            _uiState.update { it.copy(isStarting = true) }
            onlineRepository.startGame(fillWithBots, difficulty.name)
            // Reset after timeout so the button doesn't stay stuck if server doesn't respond
            delay(5000L)
            _uiState.update {
                if (it.isStarting) it.copy(isStarting = false, isStartGameTimedOut = true)
                else it
            }
        }
    }

    fun switchTeam() {
        viewModelScope.launch {
            onlineRepository.switchTeam()
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            onlineRepository.leaveRoom()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, isStartGameTimedOut = false) }
    }

    private fun updateFromRoom(room: RoomState) {
        _uiState.value = WaitingRoomUiState(
            roomCode = room.roomCode,
            players = room.players.map { p ->
                WaitingRoomPlayer(
                    id = p.id,
                    name = p.name,
                    teamId = p.teamId,
                    isHost = p.isHost,
                    isConnected = p.isConnected
                )
            },
            isHost = onlineRepository.myPlayerId == room.hostPlayerId,
            myPlayerId = onlineRepository.myPlayerId,
            targetPlayerCount = room.targetPlayerCount
        )
    }
}
