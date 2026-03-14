package com.cards.game.literature.repository

import com.cards.game.literature.model.*
import com.cards.game.literature.protocol.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    private var webSocketSession: WebSocketSession? = null
    private var connectionJob: Job? = null
    private var autoReconnectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var shouldAutoReconnect = false

    var myPlayerId: String = ""
        private set
    var roomCode: String = ""
        private set

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
    }

    suspend fun reconnect(code: String, playerId: String) {
        roomCode = code
        myPlayerId = playerId
        connectAndSend(ClientMessage.Reconnect(code, playerId))
    }

    private suspend fun connectAndSend(firstMessage: ClientMessage) {
        disconnect()
        shouldAutoReconnect = true
        _connectionState.value = ConnectionState.CONNECTING

        connectionJob = scope.launch {
            try {
                client.webSocket(urlString = "ws://$serverUrl/game") {
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

                try {
                    _connectionState.value = ConnectionState.RECONNECTING
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
        val text = json.encodeToString(message)
        session.send(Frame.Text(text))
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
            }
            is ServerMessage.Error -> {
                _errors.emit(message.message)
            }
            is ServerMessage.RoomClosed -> {
                _errors.emit("Room was closed")
                disconnect()
            }
        }
    }

    private fun applyGameView(view: PlayerGameView) {
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
        scope.cancel()
    }
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}
