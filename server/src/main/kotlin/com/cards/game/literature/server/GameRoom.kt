package com.cards.game.literature.server

import com.cards.game.literature.bot.BotAction
import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.bot.BotPlayer
import com.cards.game.literature.logic.GameEngine
import com.cards.game.literature.logic.PlayerSetupInfo
import com.cards.game.literature.model.*
import com.cards.game.literature.protocol.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class GameRoom(
    val roomCode: String,
    val targetPlayerCount: Int
) {
    private val log = LoggerFactory.getLogger("GameRoom")
    private val engine = GameEngine()
    private var botPlayer = BotPlayer()
    private val players = ConcurrentHashMap<String, PlayerSession>()
    private val playerTeams = ConcurrentHashMap<String, String>()
    private val mutex = Mutex()
    private var gameState: GameState? = null
    private var hostPlayerId: String? = null
    private var botScope: CoroutineScope? = null
    private var playerIdCounter = 0

    private var turnTimeoutJob: Job? = null
    private val disconnectJobs = ConcurrentHashMap<String, Job>()
    private val pendingReclaims = ConcurrentHashMap<String, Boolean>()
    // Tracks the player who gave the current player their turn (via a failed ask)
    private var lastAskerId: String? = null

    var phase: RoomPhase = RoomPhase.WAITING
        private set
    val createdAt: Long = System.currentTimeMillis()
    var finishedAt: Long = 0L
        private set

    companion object {
        private const val TURN_TIMEOUT_MS = 60_000L
        private const val RECONNECT_WINDOW_MS = 2 * 60_000L
    }

    fun addPlayer(name: String, isHost: Boolean = false): String {
        val playerId = "player_${playerIdCounter++}"
        val session = PlayerSession(
            playerId = playerId,
            playerName = name,
            session = null
        )
        players[playerId] = session
        playerTeams[playerId] = if (playerTeams.size % 2 == 0) "team_1" else "team_2"
        if (isHost) hostPlayerId = playerId
        return playerId
    }

    fun switchTeam(playerId: String) {
        val current = playerTeams[playerId] ?: return
        playerTeams[playerId] = if (current == "team_1") "team_2" else "team_1"
    }

    fun getPlayerSession(playerId: String): PlayerSession? = players[playerId]

    fun getHumanPlayerCount(): Int = players.size

    fun isHost(playerId: String): Boolean = playerId == hostPlayerId

    fun allDisconnected(): Boolean = players.values.all { !it.isConnected }

    fun getConnectionStatus(): Map<String, Boolean> {
        return players.mapValues { (_, session) -> session.isConnected }
    }

    private fun getDisconnectDeadlines(): Map<String, Long?> {
        return players.mapValues { (_, session) -> session.disconnectDeadline }
            .filterValues { it != null }
    }

    fun toRoomState(): RoomState {
        val roomPlayers = players.values.map { session ->
            val teamId = playerTeams[session.playerId] ?: "team_1"
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

    suspend fun startGame(fillWithBots: Boolean, botDifficultyName: String = "MEDIUM"): Boolean {
        if (phase != RoomPhase.WAITING) return false

        val difficulty = runCatching { BotDifficulty.valueOf(botDifficultyName) }.getOrDefault(BotDifficulty.MEDIUM)
        botPlayer = BotPlayer(difficulty = difficulty)

        val humanPlayers = players.values.toList()
        val setupPlayers = mutableListOf<PlayerSetupInfo>()

        // Use stored team assignments chosen in the waiting room
        humanPlayers.forEachIndexed { index, session ->
            val teamId = playerTeams[session.playerId] ?: if (index % 2 == 0) "team_1" else "team_2"
            setupPlayers.add(
                PlayerSetupInfo(
                    id = session.playerId,
                    name = session.playerName,
                    teamId = teamId,
                    isBot = false
                )
            )
        }

        // Fill remaining slots with bots, always adding to the smaller team to balance
        if (fillWithBots) {
            val botNames = listOf("Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace")
            var botIndex = 0
            while (setupPlayers.size < targetPlayerCount) {
                val t1Count = setupPlayers.count { it.teamId == "team_1" }
                val t2Count = setupPlayers.count { it.teamId == "team_2" }
                val teamId = if (t1Count <= t2Count) "team_1" else "team_2"
                setupPlayers.add(
                    PlayerSetupInfo(
                        id = "bot_$botIndex",
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
        log.info("[{}] Game started with {} players ({} human, {} bots)",
            roomCode, setupPlayers.size,
            humanPlayers.size, setupPlayers.size - humanPlayers.size)

        // Send game started to all connected players
        broadcastGameViews()

        // Start turn management
        checkNextTurn()

        return true
    }

    suspend fun processAsk(playerId: String, targetId: String, cards: List<Card>) {
        mutex.withLock {
            turnTimeoutJob?.cancel()
            var state = gameState ?: return
            if (state.phase != GamePhase.IN_PROGRESS) return
            if (state.currentPlayer.id != playerId) return

            val batchId = System.currentTimeMillis().toString()
            for (card in cards) {
                if (state.phase != GamePhase.IN_PROGRESS) break
                if (state.currentPlayer.id != playerId) break

                val result = engine.processAsk(state, playerId, targetId, card, batchId)
                state = result.newState
                gameState = state

                broadcastGameViews()
                broadcastEvents(result.events)
            }

            // If the turn moved to a different player (failed ask), the asker
            // becomes the "previous player" for timeout fallback
            if (state.currentPlayer.id != playerId) {
                lastAskerId = playerId
            }
        }
        checkNextTurn()
    }

    suspend fun processClaim(playerId: String, declaration: ClaimDeclaration) {
        mutex.withLock {
            turnTimeoutJob?.cancel()
            val state = gameState ?: return
            if (state.phase != GamePhase.IN_PROGRESS) return
            if (state.currentPlayer.id != playerId) return

            val result = engine.processClaim(state, declaration)
            gameState = result.newState

            if (result.newState.phase == GamePhase.FINISHED) {
                phase = RoomPhase.FINISHED
                finishedAt = System.currentTimeMillis()
                val t1 = result.newState.teams.firstOrNull()
                val t2 = result.newState.teams.getOrNull(1)
                log.info("[{}] Game finished — {} {} : {} {}",
                    roomCode,
                    t1?.name ?: "Team1", t1?.score ?: 0,
                    t2?.score ?: 0, t2?.name ?: "Team2")
            }

            // After a claim, there's no meaningful "previous asker" context
            lastAskerId = null

            broadcastGameViews()
            broadcastEvents(result.events)
        }
        checkNextTurn()
    }

    suspend fun handleDisconnect(playerId: String, disconnectedSession: WebSocketSession? = null) {
        val playerSession = players[playerId] ?: return

        // If the player already reconnected on a different WebSocket, ignore this
        // stale disconnect — it's the old connection's finally block firing late.
        if (disconnectedSession != null && playerSession.session != null
            && playerSession.session !== disconnectedSession
        ) {
            log.info("[{}] Ignoring stale disconnect for '{}' ({}) — already reconnected on new session",
                roomCode, playerSession.playerName, playerId)
            return
        }

        log.info("[{}] Player '{}' ({}) disconnected", roomCode, playerSession.playerName, playerId)
        playerSession.isConnected = false
        playerSession.session = null
        playerSession.lastSeen = System.currentTimeMillis()

        if (phase == RoomPhase.IN_PROGRESS) {
            // Broadcast disconnect event
            broadcastEvents(listOf(
                GameEvent.PlayerDisconnected(playerId, playerSession.playerName)
            ))

            // Set deadline and start disconnect timeout
            val deadline = System.currentTimeMillis() + RECONNECT_WINDOW_MS
            playerSession.disconnectDeadline = deadline

            broadcastGameViews()

            // Don't skip the turn immediately — let the existing turn timer
            // continue running. If the player reconnects before timeout, they
            // can still play. If the timer fires, skipTurn handles it.

            // Start 2-min disconnect timeout
            val job = botScope?.launch {
                delay(RECONNECT_WINDOW_MS)
                val session = players[playerId]
                if (session != null && !session.isConnected) {
                    replaceWithBot(playerId)
                }
            }
            if (job != null) {
                disconnectJobs[playerId] = job
            }
        } else if (phase == RoomPhase.WAITING) {
            // In waiting room, remove player and transfer host if needed
            val wasHost = playerId == hostPlayerId
            removePlayer(playerId)
            if (wasHost && players.isNotEmpty()) {
                val newHost = players.values.first()
                players.values.filter { it.isConnected }.forEach { session ->
                    botScope?.launch {
                        session.send(ServerMessage.HostTransferred(newHost.playerId, newHost.playerName))
                    } ?: run {
                        // botScope not available in waiting phase, use a temporary scope
                        CoroutineScope(Dispatchers.Default).launch {
                            session.send(ServerMessage.HostTransferred(newHost.playerId, newHost.playerName))
                        }
                    }
                }
            }
            broadcastRoomUpdate()
        }
    }

    suspend fun handleIntentionalLeave(playerId: String) {
        val playerSession = players[playerId] ?: return
        playerSession.intentionalLeave = true

        // Cancel any pending reconnect timeout
        disconnectJobs.remove(playerId)?.cancel()

        if (phase == RoomPhase.IN_PROGRESS) {
            // Immediate bot replacement, no grace period
            replaceWithBot(playerId)
        }
    }

    private suspend fun replaceWithBot(playerId: String) {
        mutex.withLock {
            val state = gameState ?: return
            val player = state.getPlayer(playerId) ?: return
            if (player.isBot) return // Already a bot
            log.info("[{}] Replacing '{}' ({}) with bot", roomCode, player.name, playerId)

            // Mark player as bot in game state
            val updatedPlayers = state.players.map {
                if (it.id == playerId) it.copy(isBot = true) else it
            }
            gameState = state.copy(players = updatedPlayers)

            // Clear disconnect deadline
            players[playerId]?.disconnectDeadline = null

            // Broadcast replacement event
            broadcastEvents(listOf(
                GameEvent.PlayerReplacedByBot(playerId, player.name)
            ))
            broadcastGameViews()
        }
        // Trigger bot turn if it's now this (now-bot) player's turn
        checkNextTurn()
    }

    suspend fun handleReconnect(playerId: String): Boolean {
        val session = players[playerId] ?: return false
        log.info("[{}] Player '{}' ({}) reconnected", roomCode, session.playerName, playerId)
        session.isConnected = true
        session.lastSeen = System.currentTimeMillis()
        session.intentionalLeave = false

        // Cancel pending disconnect timeout
        disconnectJobs.remove(playerId)?.cancel()
        session.disconnectDeadline = null

        // Check if player was already replaced by bot
        val state = gameState
        if (state != null && phase == RoomPhase.IN_PROGRESS) {
            val gamePlayer = state.getPlayer(playerId)
            if (gamePlayer != null && gamePlayer.isBot) {
                // Player was replaced by bot — queue for reclaim at next turn boundary
                pendingReclaims[playerId] = true
            }

            // Send current game state to the reconnected player FIRST, before
            // broadcasting the reconnect event. This ensures the client's event
            // replay (which filters by lastSeenEventTimestamp) processes the
            // GameUpdate before any new events update that timestamp.
            val view = state.toPlayerView(playerId, getConnectionStatus(), getDisconnectDeadlines())
            session.send(ServerMessage.GameUpdate(view))

            // Now broadcast reconnect event to all players
            broadcastEvents(listOf(
                GameEvent.PlayerReconnected(playerId, session.playerName)
            ))

            // Broadcast updated views to all
            broadcastGameViews()
        } else {
            session.send(ServerMessage.RoomUpdate(toRoomState()))
        }
        return true
    }

    private suspend fun reclaimFromBot(playerId: String) {
        mutex.withLock {
            val state = gameState ?: return
            val player = state.getPlayer(playerId) ?: return
            if (!player.isBot) return

            pendingReclaims.remove(playerId)

            val updatedPlayers = state.players.map {
                if (it.id == playerId) it.copy(isBot = false) else it
            }
            gameState = state.copy(players = updatedPlayers)
            broadcastGameViews()
        }
    }

    private suspend fun skipTurn(playerId: String) {
        mutex.withLock {
            val state = gameState ?: return
            if (state.phase != GamePhase.IN_PROGRESS) return
            if (state.currentPlayer.id != playerId) return

            val timedOutPlayer = state.currentPlayer
            val newEvents = mutableListOf<GameEvent>()

            newEvents.add(GameEvent.TurnTimedOut(playerId, timedOutPlayer.name))

            // Determine who gets the turn:
            // 1. The player who last asked this player (gave them the turn)
            // 2. Otherwise, a random active opponent
            val nextPlayerId = lastAskerId?.let { askerId ->
                val asker = state.getPlayer(askerId)
                if (asker != null && asker.isActive) askerId else null
            } ?: run {
                // Pick a random active opponent
                val opponents = state.getOpponents(playerId).filter { it.isActive }
                opponents.randomOrNull()?.id
            } ?: run {
                // Fallback: any other active player
                val nextIdx = engine.findNextActivePlayer(state.players, state.currentPlayerIndex)
                state.players[nextIdx].id
            }

            val nextPlayerIndex = state.players.indexOfFirst { it.id == nextPlayerId }
                .coerceAtLeast(0)
            val nextPlayer = state.players[nextPlayerIndex]

            newEvents.add(GameEvent.TurnChanged(nextPlayer.id, nextPlayer.name))

            lastAskerId = null

            gameState = state.copy(
                currentPlayerIndex = nextPlayerIndex,
                events = state.events + newEvents
            )

            broadcastGameViews()
            broadcastEvents(newEvents)
        }
        checkNextTurn()
    }

    private fun startTurnTimer() {
        turnTimeoutJob?.cancel()
        val state = gameState ?: return
        if (state.phase != GamePhase.IN_PROGRESS) return
        val currentId = state.currentPlayer.id
        if (state.currentPlayer.isBot) return

        turnTimeoutJob = botScope?.launch {
            delay(TURN_TIMEOUT_MS)
            skipTurn(currentId)
        }
    }

    private fun checkNextTurn() {
        val state = gameState ?: return
        if (state.phase != GamePhase.IN_PROGRESS) return
        val current = state.currentPlayer

        // Check if there's a pending reclaim for the current player
        if (current.isBot && pendingReclaims.containsKey(current.id)) {
            botScope?.launch {
                reclaimFromBot(current.id)
                startTurnTimer()
            }
            return
        }

        if (current.isBot) {
            turnTimeoutJob?.cancel()
            botScope?.launch {
                executeBotTurns()
            }
        } else {
            startTurnTimer()
        }
    }

    fun removePlayer(playerId: String) {
        players.remove(playerId)
        playerTeams.remove(playerId)
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
        val deadlines = getDisconnectDeadlines()
        players.values.filter { it.isConnected }.forEach { session ->
            val view = state.toPlayerView(session.playerId, connectionStatus, deadlines)
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

    private suspend fun executeBotTurns() {
        while (true) {
            val state = gameState ?: return
            if (state.phase != GamePhase.IN_PROGRESS) return
            val current = state.currentPlayer
            if (!current.isBot) {
                // Check for pending reclaims before starting human turn timer
                if (pendingReclaims.containsKey(current.id)) {
                    // This shouldn't normally happen (reclaim makes player non-bot),
                    // but handle gracefully
                }
                startTurnTimer()
                return
            }

            // Check for pending reclaim
            if (pendingReclaims.containsKey(current.id)) {
                reclaimFromBot(current.id)
                startTurnTimer()
                return
            }

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
        turnTimeoutJob?.cancel()
        disconnectJobs.values.forEach { it.cancel() }
        disconnectJobs.clear()
        pendingReclaims.clear()
        botScope?.cancel()
        botScope = null
    }
}
