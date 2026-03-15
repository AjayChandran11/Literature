package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cards.game.literature.repository.ConnectionState
import com.cards.game.literature.repository.OnlineGameRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LobbyUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class LobbyViewModel(
    private val onlineRepository: OnlineGameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LobbyUiState())
    val uiState: StateFlow<LobbyUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = onlineRepository.connectionState

    private val _navigateToWaitingRoom = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToWaitingRoom: Flow<String> = _navigateToWaitingRoom.asSharedFlow()

    init {
        viewModelScope.launch {
            onlineRepository.errors.collect { error ->
                _uiState.update { it.copy(errorMessage = error, isLoading = false) }
            }
        }

        viewModelScope.launch {
            onlineRepository.roomState.filterNotNull().first()
            _uiState.update { it.copy(isLoading = false) }
            _navigateToWaitingRoom.emit(onlineRepository.roomCode)
        }
    }

    fun createRoom(playerName: String, playerCount: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            onlineRepository.createRoom(playerName, playerCount)
        }
    }

    fun joinRoom(roomCode: String, playerName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            onlineRepository.joinRoom(roomCode, playerName)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
