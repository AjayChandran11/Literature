package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cards.game.literature.protocol.RoomPhase
import com.cards.game.literature.protocol.RoomState
import com.cards.game.literature.repository.OnlineGameRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WaitingRoomUiState(
    val roomCode: String = "",
    val players: List<WaitingRoomPlayer> = emptyList(),
    val isHost: Boolean = false,
    val targetPlayerCount: Int = 6,
    val errorMessage: String? = null,
    val isStarting: Boolean = false
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

    private val _uiState = MutableStateFlow(WaitingRoomUiState())
    val uiState: StateFlow<WaitingRoomUiState> = _uiState.asStateFlow()

    private val _navigateToGame = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToGame: Flow<Unit> = _navigateToGame.asSharedFlow()

    init {
        viewModelScope.launch {
            onlineRepository.roomState.filterNotNull().collect { room ->
                updateFromRoom(room)
            }
        }

        viewModelScope.launch {
            onlineRepository.gameState.filterNotNull().collect {
                _navigateToGame.emit(Unit)
            }
        }

        viewModelScope.launch {
            onlineRepository.errors.collect { error ->
                _uiState.update { it.copy(errorMessage = error, isStarting = false) }
            }
        }
    }

    fun startGame(fillWithBots: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true) }
            onlineRepository.startGame(fillWithBots)
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            onlineRepository.leaveRoom()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
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
            targetPlayerCount = room.targetPlayerCount
        )
    }
}
