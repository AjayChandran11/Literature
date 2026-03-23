package com.cards.game.literature.repository

import com.cards.game.literature.model.*
import com.cards.game.literature.network.NetworkMonitor
import com.cards.game.literature.protocol.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

sealed class PlayerConnectionEvent {
    data class Disconnected(val playerId: String, val playerName: String) : PlayerConnectionEvent()
    data class Reconnected(val playerId: String, val playerName: String) : PlayerConnectionEvent()
    data class ReplacedByBot(val playerId: String, val playerName: String) : PlayerConnectionEvent()
    data class HostChanged(val newHostName: String) : PlayerConnectionEvent()
}

data class ReconnectInfo(val playerName: String, val deadlineMs: Long)

class OnlineGameRepository(
    private val serverUrl: String,
    private val client: HttpClient
) : GameRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    private val _gameState = MutableStateFlow<GameState?>(null)
    override val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _gameEvents = MutableSharedFlow<GameEvent>(replay = 0, extraBufferCapacity = 64)
    override val gameEvents: Flow<GameEvent> = _gameEvents.asSharedFlow()

    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errors: Flow<String> = _errors.asSharedFlow()

    private val _playerEvents = MutableSharedFlow<PlayerConnectionEvent>(extraBufferCapacity = 16)
    val playerEvents: Flow<PlayerConnectionEvent> = _playerEvents.asSharedFlow()

    private val _reconnectCountdowns = MutableStateFlow<Map<String, ReconnectInfo>>(emptyMap())
    val reconnectCountdowns: StateFlow<Map<String, ReconnectInfo>> = _reconnectCountdowns.asStateFlow()

    private var webSocketSession: WebSocketSession? = null
    private var connectionJob: Job? = null
    private var autoReconnectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var shouldAutoReconnect = false
    private var lastSeenEventTimestamp: Long = 0L
    private var needsEventReplay = false

    var myPlayerId: String = ""
        private set
    var roomCode: String = ""
        private set

    init {
        scope.launch {
            NetworkMonitor.isNetworkAvailable.collect { available ->
                if (!available) {
                    // Network lost — proactively close the WebSocket so we enter
                    // RECONNECTING immediately instead of waiting for TCP timeout
                    if (_connectionState.value == ConnectionState.CONNECTED
                        && roomCode.isNotEmpty() && myPlayerId.isNotEmpty()
                    ) {
                        try { webSocketSession?.close() } catch (_: Exception) {}
                    }
                } else {
                    // Network restored — reconnect immediately
                    if (_connectionState.value != ConnectionState.CONNECTED
                        && roomCode.isNotEmpty() && myPlayerId.isNotEmpty()
                    ) {
                        autoReconnectJob?.cancel()
                        connectAndSend(ClientMessage.Reconnect(roomCode, myPlayerId))
                    }
                }
            }
        }
    }

    suspend fun createRoom(playerName: String, playerCount: Int) {
        connectAndSend(ClientMessage.CreateRoom(playerName, playerCount))
    }

    suspend fun joinRoom(code: String, playerName: String) {
        roomCode = code.uppercase()
        connectAndSend(ClientMessage.JoinRoom(roomCode, playerName))
    }

    suspend fun startGame(fillWithBots: Boolean = true) {
        sendMessage(ClientMessage.StartGame(fillWithBots))
    }

    override suspend fun createGame(playerName: String, playerCount: Int): GameState {
        // Not used for online mode
        throw UnsupportedOperationException("Use createRoom/joinRoom for online play")
    }

    override suspend fun submitAsk(askerId: String, targetId: String, card: Card) {
        sendMessage(ClientMessage.AskCards(targetId, listOf(card)))
    }

    override suspend fun submitMultiAsk(askerId: String, targetId: String, cards: List<Card>) {
        sendMessage(ClientMessage.AskCards(targetId, cards))
    }

    override suspend fun submitClaim(declaration: ClaimDeclaration) {
        sendMessage(ClientMessage.ClaimDeck(declaration))
    }

    suspend fun leaveRoom() {
        sendMessage(ClientMessage.LeaveRoom)
        disconnect()
        reset()
    }

    suspend fun leaveGame() {
        sendMessage(ClientMessage.LeaveGame)
        disconnect()
        reset()
    }

    private fun reset() {
        _gameState.value = null
        _roomState.value = null
        _reconnectCountdowns.value = emptyMap()
        lastSeenEventTimestamp = 0L
        needsEventReplay = false
        myPlayerId = ""
        roomCode = ""
    }

    suspend fun reconnect(code: String, playerId: String) {
        roomCode = code
        myPlayerId = playerId
        needsEventReplay = true
        connectAndSend(ClientMessage.Reconnect(code, playerId))
    }

    fun triggerReconnect() {
        if (roomCode.isNotEmpty() && myPlayerId.isNotEmpty()) {
            needsEventReplay = true
            scope.launch {
                connectAndSend(ClientMessage.Reconnect(roomCode, myPlayerId))
            }
        }
    }

    private suspend fun connectAndSend(firstMessage: ClientMessage) {
        // Pre-connection internet check
        if (!NetworkMonitor.isNetworkAvailable.value) {
            _errors.emit("No internet connection")
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        disconnect()
        shouldAutoReconnect = true
        _connectionState.value = ConnectionState.CONNECTING

        connectionJob = scope.launch {
            try {
                client.webSocket(urlString = "$serverUrl/game") {
                    webSocketSession = this
                    _connectionState.value = ConnectionState.CONNECTED

                    // Send the initial message
                    val text = json.encodeToString(firstMessage)
                    send(Frame.Text(text))

                    // Listen for messages
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            handleServerMessage(frame.readText())
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errors.emit("Connection error: ${e.message}")
            } finally {
                webSocketSession = null
                if (shouldAutoReconnect && roomCode.isNotEmpty() && myPlayerId.isNotEmpty()) {
                    _connectionState.value = ConnectionState.RECONNECTING
                    startAutoReconnect()
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        }
    }

    private fun startAutoReconnect() {
        autoReconnectJob?.cancel()
        autoReconnectJob = scope.launch {
            var delayMs = 1000L
            val maxDelay = 16_000L
            val maxAttempts = 10

            for (attempt in 1..maxAttempts) {
                if (!shouldAutoReconnect) return@launch
                delay(delayMs)
                if (!shouldAutoReconnect) return@launch

                // Skip attempt if no network — don't count it
                if (!NetworkMonitor.isNetworkAvailable.value) {
                    continue
                }

                try {
                    _connectionState.value = ConnectionState.RECONNECTING
                    needsEventReplay = true
                    connectAndSend(ClientMessage.Reconnect(roomCode, myPlayerId))
                    return@launch // success
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    delayMs = (delayMs * 2).coerceAtMost(maxDelay)
                }
            }
            _connectionState.value = ConnectionState.DISCONNECTED
            _errors.emit("Failed to reconnect after $maxAttempts attempts")
        }
    }

    private suspend fun sendMessage(message: ClientMessage) {
        val session = webSocketSession ?: run {
            _errors.emit("Not connected")
            return
        }
        try {
            val text = json.encodeToString(message)
            session.send(Frame.Text(text))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _errors.emit("Send failed: ${e.message}")
        }
    }

    private suspend fun handleServerMessage(text: String) {
        val message = try {
            json.decodeFromString<ServerMessage>(text)
        } catch (e: Exception) {
            _errors.emit("Invalid server message")
            return
        }

        when (message) {
            is ServerMessage.RoomCreated -> {
                roomCode = message.roomCode
                myPlayerId = message.playerId
            }
            is ServerMessage.RoomUpdate -> {
                _roomState.value = message.room
            }
            is ServerMessage.GameStarted -> {
                applyGameView(message.view)
            }
            is ServerMessage.GameUpdate -> {
                applyGameView(message.view)
            }
            is ServerMessage.GameEventOccurred -> {
                _gameEvents.emit(message.event)
                lastSeenEventTimestamp = message.event.timestamp

                // Also emit player connection events for UI notifications
                when (val event = message.event) {
                    is GameEvent.PlayerDisconnected -> {
                        _playerEvents.emit(
                            PlayerConnectionEvent.Disconnected(event.playerId, event.playerName)
                        )
                        // Show reconnect countdown immediately (estimate 2-min deadline)
                        val deadline = event.timestamp + 2 * 60_000L
                        _reconnectCountdowns.update { current ->
                            current + (event.playerId to ReconnectInfo(event.playerName, deadline))
                        }
                    }
                    is GameEvent.PlayerReconnected -> {
                        _playerEvents.emit(
                            PlayerConnectionEvent.Reconnected(event.playerId, event.playerName)
                        )
                        // Clear countdown immediately
                        _reconnectCountdowns.update { current ->
                            current - event.playerId
                        }
                    }
                    is GameEvent.PlayerReplacedByBot -> {
                        _playerEvents.emit(
                            PlayerConnectionEvent.ReplacedByBot(event.playerId, event.playerName)
                        )
                        // Clear countdown — player was replaced
                        _reconnectCountdowns.update { current ->
                            current - event.playerId
                        }
                    }
                    else -> {}
                }
            }
            is ServerMessage.Error -> {
                _errors.emit(message.message)
            }
            is ServerMessage.RoomClosed -> {
                _errors.emit("Room was closed")
                disconnect()
            }
            is ServerMessage.HostTransferred -> {
                _playerEvents.emit(
                    PlayerConnectionEvent.HostChanged(message.newHostName)
                )
            }
        }
    }

    private suspend fun applyGameView(view: PlayerGameView) {
        // Convert PlayerGameView into a synthetic GameState
        // The view has our hand but only card counts for others
        val players = view.players.map { info ->
            if (info.id == view.myPlayerId) {
                Player(
                    id = info.id,
                    name = info.name,
                    teamId = info.teamId,
                    hand = view.myHand,
                    isBot = info.isBot
                )
            } else {
                // Create placeholder hand with correct count (cards hidden)
                Player(
                    id = info.id,
                    name = info.name,
                    teamId = info.teamId,
                    hand = (1..info.cardCount).map {
                        Card(Suit.SPADES, CardValue.ACE) // placeholder
                    },
                    isBot = info.isBot
                )
            }
        }

        val currentPlayerIndex = players.indexOfFirst { it.id == view.currentPlayerId }
            .coerceAtLeast(0)

        val syntheticState = GameState(
            gameId = "online_${roomCode}",
            players = players,
            teams = view.teams,
            currentPlayerIndex = currentPlayerIndex,
            phase = view.phase,
            halfSuitStatuses = view.halfSuitStatuses,
            events = view.recentEvents,
            playerCount = players.size
        )

        _gameState.value = syntheticState

        // On reconnect, replay events we missed while disconnected into _gameEvents
        // so the ViewModel's gameLog catches up. Only do this once after reconnecting,
        // not on every GameUpdate (which would duplicate events already arriving via
        // GameEventOccurred).
        if (needsEventReplay) {
            needsEventReplay = false
            val missedEvents = view.recentEvents.filter { it.timestamp > lastSeenEventTimestamp }
            for (event in missedEvents) {
                _gameEvents.emit(event)
            }
            if (view.recentEvents.isNotEmpty()) {
                lastSeenEventTimestamp = view.recentEvents.maxOf { it.timestamp }
            }
        }

        // Update reconnect countdowns from player info
        val countdowns = mutableMapOf<String, ReconnectInfo>()
        for (info in view.players) {
            val deadline = info.reconnectDeadlineMs
            if (info.isPendingReconnect && deadline != null) {
                countdowns[info.id] = ReconnectInfo(info.name, deadline)
            }
        }
        _reconnectCountdowns.value = countdowns
    }

    fun disconnect() {
        shouldAutoReconnect = false
        autoReconnectJob?.cancel()
        autoReconnectJob = null
        connectionJob?.cancel()
        connectionJob = null
        webSocketSession = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun cleanup() {
        disconnect()
        // Do NOT cancel scope — it is application-lifetime (singleton). Cancelling it
        // permanently breaks connectAndSend for any subsequent online session.
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}
