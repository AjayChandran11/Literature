package com.cards.game.literature.repository

import co.touchlab.kermit.Logger
import com.cards.game.literature.bot.BotAction
import com.cards.game.literature.bot.BotPlayer
import com.cards.game.literature.logic.GameEngine
import com.cards.game.literature.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalGameRepository(
    private val gameEngine: GameEngine = GameEngine(),
    private val botPlayer: BotPlayer = BotPlayer()
) : GameRepository {

    private val log = Logger.withTag("LocalGameRepo")

    private val _gameState = MutableStateFlow<GameState?>(null)
    override val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _gameEvents = MutableSharedFlow<GameEvent>(replay = 0, extraBufferCapacity = 64)
    override val gameEvents: Flow<GameEvent> = _gameEvents.asSharedFlow()

    private var botScope: CoroutineScope? = null
    private val mutex = Mutex()
    private var botJobRunning = false

    override suspend fun createGame(playerName: String, playerCount: Int): GameState {
        botScope?.cancel()
        botScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        botJobRunning = false

        val gameId = "game_${currentTimeMillis()}"
        log.i { "Creating local game: player=$playerName, count=$playerCount" }
        val state = gameEngine.createGame(gameId, playerName, playerCount)
        _gameState.value = state
        _gameEvents.emit(GameEvent.GameStarted(playerCount))

        triggerBotTurnIfNeeded()

        return state
    }

    override suspend fun submitAsk(askerId: String, targetId: String, card: Card) {
        mutex.withLock {
            val currentState = _gameState.value ?: return
            val result = gameEngine.processAsk(currentState, askerId, targetId, card)
            _gameState.value = result.newState
            result.events.forEach { _gameEvents.emit(it) }
        }
        triggerBotTurnIfNeeded()
    }

    override suspend fun submitMultiAsk(askerId: String, targetId: String, cards: List<Card>) {
        mutex.withLock {
            var currentState = _gameState.value ?: return
            val batchId = currentTimeMillis().toString()
            for (card in cards) {
                if (currentState.phase != GamePhase.IN_PROGRESS) break
                val result = gameEngine.processAsk(currentState, askerId, targetId, card, batchId)
                currentState = result.newState
                _gameState.value = currentState
                result.events.forEach { _gameEvents.emit(it) }
                // Stop if the turn passed away from the asker
                if (currentState.currentPlayer.id != askerId) break
            }
        }
        triggerBotTurnIfNeeded()
    }

    override suspend fun submitClaim(declaration: ClaimDeclaration) {
        mutex.withLock {
            val currentState = _gameState.value ?: return
            val result = gameEngine.processClaim(currentState, declaration)
            _gameState.value = result.newState
            result.events.forEach { _gameEvents.emit(it) }
        }
        triggerBotTurnIfNeeded()
    }

    private fun triggerBotTurnIfNeeded() {
        val state = _gameState.value ?: return
        if (state.phase != GamePhase.IN_PROGRESS) return
        val current = state.currentPlayer
        if (!current.isBot) return
        if (botJobRunning) return

        botJobRunning = true
        botScope?.launch {
            try {
                executeBotTurns()
            } finally {
                botJobRunning = false
            }
        }
    }

    private suspend fun executeBotTurns() {
        while (true) {
            val state = _gameState.value ?: return
            if (state.phase != GamePhase.IN_PROGRESS) return
            val current = state.currentPlayer
            if (!current.isBot) return

            val action = botPlayer.decideMove(state, current.id)
            log.d { "Bot ${current.name} action: ${action::class.simpleName}" }
            when (action) {
                is BotAction.Ask -> {
                    mutex.withLock {
                        val freshState = _gameState.value ?: return
                        if (freshState.phase != GamePhase.IN_PROGRESS) return
                        if (freshState.currentPlayer.id != current.id) return
                        val result = gameEngine.processAsk(freshState, current.id, action.targetId, action.card)
                        _gameState.value = result.newState
                        result.events.forEach { _gameEvents.emit(it) }
                    }
                }
                is BotAction.Claim -> {
                    mutex.withLock {
                        val freshState = _gameState.value ?: return
                        if (freshState.phase != GamePhase.IN_PROGRESS) return
                        if (freshState.currentPlayer.id != current.id) return
                        val result = gameEngine.processClaim(freshState, action.declaration)
                        _gameState.value = result.newState
                        result.events.forEach { _gameEvents.emit(it) }
                    }
                }
            }
        }
    }

    fun cleanup() {
        botScope?.cancel()
        botScope = null
        botJobRunning = false
        _gameState.value = null
    }
}
