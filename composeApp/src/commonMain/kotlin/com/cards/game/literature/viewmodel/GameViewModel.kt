package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.CardTrackerState
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.*
import com.cards.game.literature.repository.GameRepository
import com.cards.game.literature.repository.LocalGameRepository
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.stats.MatchRecord
import com.cards.game.literature.stats.Outcome
import com.cards.game.literature.stats.StatsStore
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
    val isOnline: Boolean = false,
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
    private val repository: GameRepository,
    private val overridePlayerId: String? = null
) : ViewModel() {

    private val log = Logger.withTag("GameViewModel")

    override fun onCleared() {
        super.onCleared()
        when (repository) {
            is LocalGameRepository -> repository.cleanup()
            is OnlineGameRepository -> repository.cleanup()
        }
    }

    private val isOnline = repository is OnlineGameRepository
    private val _uiState = MutableStateFlow(GameUiState(isOnline = isOnline))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _gameLog = MutableStateFlow<List<GameEvent>>(emptyList())
    val gameLog: StateFlow<List<GameEvent>> = _gameLog.asStateFlow()

    private val _trackerState = MutableStateFlow(CardTrackerState())
    val trackerState: StateFlow<CardTrackerState> = _trackerState.asStateFlow()

    private val cardTracker = CardTracker()
    private var myPlayerId = overridePlayerId ?: "player_0"

    // Per-game stat counters, tracked incrementally because the online
    // GameState only carries the last ~20 events — totals can't be derived
    // from the final state.
    private var myAsks = 0
    private var myAsksSuccessful = 0
    private var myClaims = 0
    private var myClaimsCorrect = 0
    private var recordedGameId: String? = null
    private var lastDifficulty: BotDifficulty? = null

    fun setPlayerId(playerId: String) {
        myPlayerId = playerId
    }

    init {
        viewModelScope.launch {
            repository.gameState.filterNotNull().collect { state ->
                updateUiState(state)
                if (state.phase == GamePhase.FINISHED) {
                    maybeRecordGame(state)
                }
            }
        }
        viewModelScope.launch {
            repository.gameEvents.collect { event ->
                _gameLog.update { it + event }
                trackMyActions(event)
            }
        }
    }

    private fun trackMyActions(event: GameEvent) {
        when (event) {
            is GameEvent.CardAsked -> if (event.askerId == myPlayerId) {
                myAsks++
                if (event.success) myAsksSuccessful++
            }
            is GameEvent.DeckClaimed -> if (event.claimerId == myPlayerId) {
                myClaims++
                if (event.correct) myClaimsCorrect++
            }
            else -> {}
        }
    }

    private suspend fun maybeRecordGame(state: GameState) {
        if (recordedGameId == state.gameId) return
        recordedGameId = state.gameId

        val myTeam = state.getTeamForPlayer(myPlayerId) ?: return
        val opponentTeam = state.teams.firstOrNull { it.id != myTeam.id } ?: return
        val outcome = when {
            myTeam.score > opponentTeam.score -> Outcome.WIN
            myTeam.score < opponentTeam.score -> Outcome.LOSS
            else -> Outcome.DRAW
        }
        val result = StatsStore.recordGame(
            gameId = state.gameId,
            record = MatchRecord(
                timestamp = currentTimeMillis(),
                isOnline = isOnline,
                playerCount = state.playerCount,
                botDifficulty = if (isOnline) null else lastDifficulty?.name,
                myScore = myTeam.score,
                opponentScore = opponentTeam.score,
                outcome = outcome,
                myAsks = myAsks,
                myAsksSuccessful = myAsksSuccessful,
                myClaims = myClaims,
                myClaimsCorrect = myClaimsCorrect
            )
        )
        // Newly unlocked achievements travel to the result screen via
        // StatsStore.pendingCelebration — this ViewModel is popped off the
        // back stack before the result screen exists.
        if (result != null) {
            log.i { "Recorded ${outcome.name} for game ${state.gameId}, unlocked: ${result.newlyUnlocked}" }
        }
    }

    fun startGame(playerName: String, playerCount: Int, difficulty: BotDifficulty = BotDifficulty.MEDIUM) {
        lastDifficulty = difficulty
        myAsks = 0
        myAsksSuccessful = 0
        myClaims = 0
        myClaimsCorrect = 0
        recordedGameId = null
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.createGame(playerName, playerCount, difficulty)
            } catch (e: Exception) {
                log.e(e) { "Failed to start game" }
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
                log.d { "Asking $targetId for ${cards.size} card(s)" }
                repository.submitMultiAsk(myPlayerId, targetId, cards)
            } catch (e: Exception) {
                log.e(e) { "Ask failed" }
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun claimDeck(declaration: ClaimDeclaration) {
        viewModelScope.launch {
            try {
                log.d { "Claiming ${declaration.halfSuit}" }
                repository.submitClaim(declaration)
            } catch (e: Exception) {
                log.e(e) { "Claim failed" }
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
            isOnline = isOnline,
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
