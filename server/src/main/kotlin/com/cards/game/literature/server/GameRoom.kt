package com.cards.game.literature.server

import com.cards.game.literature.bot.BotAction
import com.cards.game.literature.bot.BotPlayer
import com.cards.game.literature.logic.GameEngine
import com.cards.game.literature.logic.PlayerSetupInfo
import com.cards.game.literature.model.*
import com.cards.game.literature.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class GameRoom(
    val roomCode: String,
    val targetPlayerCount: Int
) {
    private val engine = GameEngine()
    private val botPlayer = BotPlayer()
    private val players = ConcurrentHashMap<String, PlayerSession>()
    private val mutex = Mutex()
    private var gameState: GameState? = null
    private var hostPlayerId: String? = null
    private var botScope: CoroutineScope? = null
    private var playerIdCounter = 0

    var phase: RoomPhase = RoomPhase.WAITING
        private set
    val createdAt: Long = System.currentTimeMillis()
    var finishedAt: Long = 0L
        private set

    fun addPlayer(name: String, isHost: Boolean = false): String {
        val playerId = "player_${playerIdCounter++}"
        val session = PlayerSession(
            playerId = playerId,
            playerName = name,
            session = null
        )
        players[playerId] = session
        if (isHost) hostPlayerId = playerId
        return playerId
    }

    fun getPlayerSession(playerId: String): PlayerSession? = players[playerId]

    fun getHumanPlayerCount(): Int = players.size

    fun isHost(playerId: String): Boolean = playerId == hostPlayerId

    fun allDisconnected(): Boolean = players.values.all { !it.isConnected }

    fun getConnectionStatus(): Map<String, Boolean> {
        return players.mapValues { (_, session) -> session.isConnected }
    }

    fun toRoomState(): RoomState {
        val roomPlayers = players.values.map { session ->
            val teamId = if (players.keys.toList().indexOf(session.playerId) % 2 == 0) "team_1" else "team_2"
            RoomPlayerInfo(
                id = session.playerId,
                name = session.playerName,
                teamId = teamId,
                isBot = false,
                isConnected = session.isConnected,
                isHost = session.playerId == hostPlayerId
            )
        }
        return RoomState(
            roomCode = roomCode,
            phase = phase,
            players = roomPlayers,
            hostPlayerId = hostPlayerId ?: "",
            targetPlayerCount = targetPlayerCount
        )
    }

    suspend fun startGame(fillWithBots: Boolean): Boolean {
        if (phase != RoomPhase.WAITING) return false

        val humanPlayers = players.values.toList()
        val setupPlayers = mutableListOf<PlayerSetupInfo>()

        // Assign teams alternating
        humanPlayers.forEachIndexed { index, session ->
            val teamId = if (index % 2 == 0) "team_1" else "team_2"
            setupPlayers.add(
                PlayerSetupInfo(
                    id = session.playerId,
                    name = session.playerName,
                    teamId = teamId,
                    isBot = false
                )
            )
        }

        // Fill remaining slots with bots
        if (fillWithBots) {
            val botNames = listOf("Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace")
            var botIndex = 0
            while (setupPlayers.size < targetPlayerCount) {
                val botId = "bot_${botIndex}"
                val teamId = if (setupPlayers.size % 2 == 0) "team_1" else "team_2"
                setupPlayers.add(
                    PlayerSetupInfo(
                        id = botId,
                        name = botNames.getOrElse(botIndex) { "Bot ${botIndex + 1}" },
                        teamId = teamId,
                        isBot = true
                    )
                )
                botIndex++
            }
        } else if (humanPlayers.size < targetPlayerCount) {
            return false // Not enough players
        }

        val gameId = "game_${roomCode}_${System.currentTimeMillis()}"
        gameState = engine.createMultiplayerGame(gameId, setupPlayers)
        phase = RoomPhase.IN_PROGRESS

        botScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // Send game started to all connected players
        broadcastGameViews()

        // Trigger bot turns if needed
        triggerBotTurnIfNeeded()

        return true
    }

    suspend fun processAsk(playerId: String, targetId: String, cards: List<Card>) {
        mutex.withLock {
            var state = gameState ?: return
            if (state.phase != GamePhase.IN_PROGRESS) return
            if (state.currentPlayer.id != playerId) return

            for (card in cards) {
                if (state.phase != GamePhase.IN_PROGRESS) break
                if (state.currentPlayer.id != playerId) break

                val result = engine.processAsk(state, playerId, targetId, card)
                state = result.newState
                gameState = state

                broadcastGameViews()
                broadcastEvents(result.events)
            }
        }
        triggerBotTurnIfNeeded()
    }

    suspend fun processClaim(playerId: String, declaration: ClaimDeclaration) {
        mutex.withLock {
            val state = gameState ?: return
            if (state.phase != GamePhase.IN_PROGRESS) return
            if (state.currentPlayer.id != playerId) return

            val result = engine.processClaim(state, declaration)
            gameState = result.newState

            if (result.newState.phase == GamePhase.FINISHED) {
                phase = RoomPhase.FINISHED
                finishedAt = System.currentTimeMillis()
            }

            broadcastGameViews()
            broadcastEvents(result.events)
        }
        triggerBotTurnIfNeeded()
    }

    fun handleDisconnect(playerId: String) {
        players[playerId]?.apply {
            isConnected = false
            session = null
            lastSeen = System.currentTimeMillis()
        }

        // If game is in progress, start a timeout to replace with bot
        if (phase == RoomPhase.IN_PROGRESS) {
            botScope?.launch {
                delay(2 * 60_000) // 2 minute reconnect window
                val session = players[playerId]
                if (session != null && !session.isConnected) {
                    replaceWithBot(playerId)
                }
            }
        }
    }

    private suspend fun replaceWithBot(playerId: String) {
        mutex.withLock {
            val state = gameState ?: return
            val player = state.getPlayer(playerId) ?: return

            // Mark player as bot in game state
            val updatedPlayers = state.players.map {
                if (it.id == playerId) it.copy(isBot = true) else it
            }
            gameState = state.copy(players = updatedPlayers)
            broadcastRoomUpdate()
        }
        // Trigger bot turn if it's now this (now-bot) player's turn
        triggerBotTurnIfNeeded()
    }

    suspend fun handleReconnect(playerId: String): Boolean {
        val session = players[playerId] ?: return false
        session.isConnected = true
        session.lastSeen = System.currentTimeMillis()

        // Send current game state
        val state = gameState
        if (state != null && phase == RoomPhase.IN_PROGRESS) {
            val view = state.toPlayerView(playerId, getConnectionStatus())
            session.send(ServerMessage.GameUpdate(view))
        } else {
            session.send(ServerMessage.RoomUpdate(toRoomState()))
        }
        return true
    }

    fun removePlayer(playerId: String) {
        players.remove(playerId)
        if (playerId == hostPlayerId && players.isNotEmpty()) {
            hostPlayerId = players.keys.first()
        }
    }

    suspend fun broadcastRoomUpdate() {
        val roomState = toRoomState()
        players.values.filter { it.isConnected }.forEach { session ->
            session.send(ServerMessage.RoomUpdate(roomState))
        }
    }

    private suspend fun broadcastGameViews() {
        val state = gameState ?: return
        val connectionStatus = getConnectionStatus()
        players.values.filter { it.isConnected }.forEach { session ->
            val view = state.toPlayerView(session.playerId, connectionStatus)
            session.send(ServerMessage.GameUpdate(view))
        }
    }

    private suspend fun broadcastEvents(events: List<GameEvent>) {
        players.values.filter { it.isConnected }.forEach { session ->
            events.forEach { event ->
                session.send(ServerMessage.GameEventOccurred(event))
            }
        }
    }

    private fun triggerBotTurnIfNeeded() {
        val state = gameState ?: return
        if (state.phase != GamePhase.IN_PROGRESS) return
        val current = state.currentPlayer
        if (!current.isBot) return

        botScope?.launch {
            executeBotTurns()
        }
    }

    private suspend fun executeBotTurns() {
        while (true) {
            val state = gameState ?: return
            if (state.phase != GamePhase.IN_PROGRESS) return
            val current = state.currentPlayer
            if (!current.isBot) return

            val action = botPlayer.decideMove(state, current.id)
            when (action) {
                is BotAction.Ask -> {
                    mutex.withLock {
                        val freshState = gameState ?: return
                        if (freshState.phase != GamePhase.IN_PROGRESS) return
                        if (freshState.currentPlayer.id != current.id) return
                        val result = engine.processAsk(freshState, current.id, action.targetId, action.card)
                        gameState = result.newState

                        if (result.newState.phase == GamePhase.FINISHED) {
                            phase = RoomPhase.FINISHED
                            finishedAt = System.currentTimeMillis()
                        }

                        broadcastGameViews()
                        broadcastEvents(result.events)
                    }
                }
                is BotAction.Claim -> {
                    mutex.withLock {
                        val freshState = gameState ?: return
                        if (freshState.phase != GamePhase.IN_PROGRESS) return
                        if (freshState.currentPlayer.id != current.id) return
                        val result = engine.processClaim(freshState, action.declaration)
                        gameState = result.newState

                        if (result.newState.phase == GamePhase.FINISHED) {
                            phase = RoomPhase.FINISHED
                            finishedAt = System.currentTimeMillis()
                        }

                        broadcastGameViews()
                        broadcastEvents(result.events)
                    }
                }
            }
        }
    }

    fun cleanup() {
        botScope?.cancel()
        botScope = null
    }
}
