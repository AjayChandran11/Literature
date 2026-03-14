package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.CardTrackerState
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.*
import com.cards.game.literature.repository.GameRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlayerInfo(
    val id: String,
    val name: String,
    val cardCount: Int,
    val isActive: Boolean,
    val isCurrentTurn: Boolean
)

data class GameUiState(
    val isMyTurn: Boolean = false,
    val myHand: List<Card> = emptyList(),
    val myHandByHalfSuit: Map<HalfSuit, List<Card>> = emptyMap(),
    val opponents: List<PlayerInfo> = emptyList(),
    val teammates: List<PlayerInfo> = emptyList(),
    val myTeamScore: Int = 0,
    val opponentTeamScore: Int = 0,
    val halfSuitStatuses: List<HalfSuitStatus> = emptyList(),
    val phase: GamePhase = GamePhase.WAITING,
    val activePlayerName: String = "",
    val activePlayerId: String = "",
    val isLoading: Boolean = false,
    val isBotThinking: Boolean = false,
    val errorMessage: String? = null,
    val myPlayerId: String = "player_0",
    val myTeamId: String = "team_1"
)

class GameViewModel(
    private val repository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _gameLog = MutableStateFlow<List<GameEvent>>(emptyList())
    val gameLog: StateFlow<List<GameEvent>> = _gameLog.asStateFlow()

    private val _trackerState = MutableStateFlow(CardTrackerState())
    val trackerState: StateFlow<CardTrackerState> = _trackerState.asStateFlow()

    private val cardTracker = CardTracker()
    private val myPlayerId = "player_0"

    init {
        viewModelScope.launch {
            repository.gameState.filterNotNull().collect { state ->
                updateUiState(state)
            }
        }
        viewModelScope.launch {
            repository.gameEvents.collect { event ->
                _gameLog.update { it + event }
            }
        }
    }

    fun startGame(playerName: String, playerCount: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.createGame(playerName, playerCount)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    fun askCard(targetId: String, card: Card) {
        viewModelScope.launch {
            try {
                repository.submitAsk(myPlayerId, targetId, card)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun askCards(targetId: String, cards: List<Card>) {
        viewModelScope.launch {
            try {
                repository.submitMultiAsk(myPlayerId, targetId, cards)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun claimDeck(declaration: ClaimDeclaration) {
        viewModelScope.launch {
            try {
                repository.submitClaim(declaration)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun updateUiState(state: GameState) {
        val me = state.getPlayer(myPlayerId)
        val myTeam = state.getTeamForPlayer(myPlayerId)
        val opponentTeam = state.teams.firstOrNull { it.id != myTeam?.id }

        val myHand = me?.hand ?: emptyList()
        val handByHalfSuit = myHand.groupBy { DeckUtils.getHalfSuit(it) }

        val opponents = state.getOpponents(myPlayerId).map { player ->
            PlayerInfo(
                id = player.id,
                name = player.name,
                cardCount = player.cardCount,
                isActive = player.isActive,
                isCurrentTurn = state.currentPlayer.id == player.id
            )
        }

        val teammates = state.getTeammates(myPlayerId).map { player ->
            PlayerInfo(
                id = player.id,
                name = player.name,
                cardCount = player.cardCount,
                isActive = player.isActive,
                isCurrentTurn = state.currentPlayer.id == player.id
            )
        }

        val tracker = cardTracker.buildState(state.events, state.players, myPlayerId)
        _trackerState.value = tracker

        _uiState.value = GameUiState(
            isMyTurn = state.currentPlayer.id == myPlayerId,
            myHand = myHand,
            myHandByHalfSuit = handByHalfSuit,
            opponents = opponents,
            teammates = teammates,
            myTeamScore = myTeam?.score ?: 0,
            opponentTeamScore = opponentTeam?.score ?: 0,
            halfSuitStatuses = state.halfSuitStatuses,
            phase = state.phase,
            activePlayerName = state.currentPlayer.name,
            activePlayerId = state.currentPlayer.id,
            isLoading = false,
            isBotThinking = state.currentPlayer.isBot && state.phase == GamePhase.IN_PROGRESS,
            myPlayerId = myPlayerId,
            myTeamId = myTeam?.id ?: "team_1"
        )
    }
}
