package com.cards.game.literature.repository

import co.touchlab.kermit.Logger
import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.model.*
import com.cards.game.literature.rethrowIfPlatformFatal
import com.cards.game.literature.network.NetworkMonitor
import com.cards.game.literature.preferences.OnlineSessionBackup
import com.cards.game.literature.protocol.*
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

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

    private val log = Logger.withTag("OnlineGameRepo")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    private val _gameState = MutableStateFlow<GameState?>(null)
    override val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _gameEvents = MutableSharedFlow<GameEvent>(replay = 0, extraBufferCapacity = 64)
    override val gameEvents: Flow<GameEvent> = _gameEvents.asSharedFlow()

    // Full per-match event log, accumulated CLIENT-side. The wire view carries only
    // the last ~20 events (PlayerGameView.recentEvents), which is all the synthetic
    // GameState — and therefore the result screen — would otherwise ever see. The
    // events streamed over the whole match are the complete story (minus anything
    // missed while disconnected longer than the replay window). Copy-on-write list:
    // appended from the socket loop, snapshot-read by ResultViewModel at construction;
    // reset whenever a view for a NEW gameId arrives (rematch, next room).
    private var _eventLog: List<GameEvent> = emptyList()
    private var eventLogGameId: String? = null
    val eventLog: List<GameEvent> get() = _eventLog

    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val errors: Flow<String> = _errors.asSharedFlow()

    // Set when the online session ends in a way reconnecting can't fix: the client is rejected
    // (protocol too old) or the room no longer exists (server restarted and lost it). The UI
    // observes this to leave the game with an explanation instead of freezing on a stale board.
    private val _fatalError = MutableStateFlow<FatalSessionError?>(null)
    val fatalError: StateFlow<FatalSessionError?> = _fatalError.asStateFlow()

    private val _playerEvents = MutableSharedFlow<PlayerConnectionEvent>(extraBufferCapacity = 16)
    val playerEvents: Flow<PlayerConnectionEvent> = _playerEvents.asSharedFlow()

    private val _reconnectCountdowns = MutableStateFlow<Map<String, ReconnectInfo>>(emptyMap())
    val reconnectCountdowns: StateFlow<Map<String, ReconnectInfo>> = _reconnectCountdowns.asStateFlow()

    private val _reactions = MutableSharedFlow<ServerMessage.ReactionReceived>(
        replay = 0, extraBufferCapacity = 32
    )
    val reactions: Flow<ServerMessage.ReactionReceived> = _reactions.asSharedFlow()

    // Fired when the host resets the room for a rematch — the result screen
    // navigates everyone back to the waiting room.
    private val _rematchStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val rematchStarted: Flow<Unit> = _rematchStarted.asSharedFlow()

    private var webSocketSession: WebSocketSession? = null
    private var connectionJob: Job? = null
    private var autoReconnectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var shouldAutoReconnect = false
    private var lastSeenEventTimestamp: Long = 0L

    // Events spliced into the log from a FINISHED view before their GameEventOccurred
    // broadcasts arrived (the server sends views before events) — the copies are dropped
    // on arrival by matching this queue's head. See the game-end splice in applyGameView.
    private val pendingSplicedEvents = ArrayDeque<GameEvent>()
    private val needsEventReplay = MutableStateFlow(false)

    // Survives across connection failures so backoff genuinely grows; reset
    // only on a confirmed connection or after giving up. NOT reset in
    // disconnect() — connectAndSend() calls disconnect() on every retry.
    private var reconnectAttempts = 0

    private companion object {
        const val MAX_RECONNECT_ATTEMPTS = 10
        const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        const val MAX_RECONNECT_DELAY_MS = 16_000L
    }

    var myPlayerId: String = ""
        private set
    var roomCode: String = ""
        private set
    // Proof of identity for reconnects, issued by the server in RoomCreated.
    private var reconnectToken: String = ""

    // The game a web tab-refresh resumed into — its match intro already played pre-refresh,
    // so the board must not replay it. Set on the first game view after resumeSession().
    var resumedGameId: String? = null
        private set
    private var markNextGameAsResumed = false

    init {
        scope.launch {
            NetworkMonitor.isNetworkAvailable.collect { available ->
                if (!available) {
                    // Network lost — proactively close the WebSocket so we enter
                    // RECONNECTING immediately instead of waiting for TCP timeout
                    if (_connectionState.value == ConnectionState.CONNECTED
                        && roomCode.isNotEmpty() && myPlayerId.isNotEmpty()
                    ) {
                        try { webSocketSession?.close() } catch (_: Throwable) {}
                    }
                } else {
                    // Network restored — reconnect immediately. Gated on shouldAutoReconnect so a
                    // session we deliberately ended (left, or a terminal fatal error like the room
                    // being gone) is NOT silently re-established on the next connectivity change.
                    if (shouldAutoReconnect
                        && _connectionState.value != ConnectionState.CONNECTED
                        && roomCode.isNotEmpty() && myPlayerId.isNotEmpty()
                    ) {
                        autoReconnectJob?.cancel()
                        needsEventReplay.value = true
                        connectAndSend(ClientMessage.Reconnect(roomCode, myPlayerId, reconnectToken))
                    }
                }
            }
        }
    }

    suspend fun warmUp() {
        val httpUrl = when {
            serverUrl.startsWith("wss://") -> "https://" + serverUrl.removePrefix("wss://")
            serverUrl.startsWith("ws://") -> "http://" + serverUrl.removePrefix("ws://")
            else -> serverUrl
        }
        try {
            log.i { "Warming up server: $httpUrl/health" }
            client.get("$httpUrl/health") {
                timeout {
                    connectTimeoutMillis = 65_000
                    socketTimeoutMillis = 65_000
                    requestTimeoutMillis = 65_000
                }
            }
            log.i { "Server warm-up successful" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) { // not Exception: Kotlin/Wasm wraps JS errors in JsException : Throwable
            rethrowIfPlatformFatal(e) // NOT `catch (Error)`: ktor wasm wraps fetch failures in kotlin.Error
            log.w { "Server warm-up finished (${e.message})" }
        }
    }

    suspend fun createRoom(playerName: String, playerCount: Int) {
        log.i { "Creating room: player=$playerName, count=$playerCount" }
        _fatalError.value = null
        connectAndSend(ClientMessage.CreateRoom(playerName, playerCount))
    }

    suspend fun joinRoom(code: String, playerName: String) {
        roomCode = code.uppercase()
        log.i { "Joining room: code=$roomCode, player=$playerName" }
        _fatalError.value = null
        connectAndSend(ClientMessage.JoinRoom(roomCode, playerName))
    }

    /** Resumes a session restored from [OnlineSessionBackup] (web tab refresh). */
    suspend fun resumeSession(code: String, playerId: String, token: String) {
        roomCode = code
        myPlayerId = playerId
        reconnectToken = token
        _fatalError.value = null
        needsEventReplay.value = true
        markNextGameAsResumed = true
        log.i { "Resuming session: code=$roomCode, playerId=$myPlayerId" }
        connectAndSend(ClientMessage.Reconnect(roomCode, myPlayerId, token))
    }

    suspend fun startGame(fillWithBots: Boolean = true, botDifficulty: String = "MEDIUM") {
        sendMessage(ClientMessage.StartGame(fillWithBots, botDifficulty))
    }

    suspend fun switchTeam() {
        sendMessage(ClientMessage.SwitchTeam)
    }

    suspend fun sendReaction(reaction: ReactionType) {
        sendMessage(ClientMessage.SendReaction(reaction))
    }

    suspend fun requestRematch() {
        sendMessage(ClientMessage.Rematch)
    }

    override suspend fun createGame(playerName: String, playerCount: Int, difficulty: BotDifficulty): GameState {
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

    override suspend fun submitPassTarget(selectedPlayerId: String) {
        sendMessage(ClientMessage.SelectPassTarget(selectedPlayerId))
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

    /**
     * Fire-and-forget leave for use from a ViewModel's onCleared (where the caller's scope
     * is being cancelled): tells the server, disconnects, and clears identity via reset() so
     * the NetworkMonitor auto-reconnect can't re-attach to the abandoned room after a blip.
     * Runs on the repository's app-lifetime scope.
     */
    fun leaveRoomAndReset() {
        scope.launch { leaveRoom() }
    }

    private fun reset() {
        OnlineSessionBackup.clear()
        _gameState.value = null
        _roomState.value = null
        _reconnectCountdowns.value = emptyMap()
        _fatalError.value = null
        lastSeenEventTimestamp = 0L
        pendingSplicedEvents.clear()
        needsEventReplay.value = false
        myPlayerId = ""
        roomCode = ""
        reconnectToken = ""
        reconnectAttempts = 0
        resumedGameId = null
        markNextGameAsResumed = false
    }

    suspend fun reconnect(code: String, playerId: String) {
        roomCode = code
        myPlayerId = playerId
        needsEventReplay.value = true
        connectAndSend(ClientMessage.Reconnect(code, playerId, reconnectToken))
    }

    fun triggerReconnect() {
        if (roomCode.isNotEmpty() && myPlayerId.isNotEmpty()) {
            needsEventReplay.value = true
            scope.launch {
                connectAndSend(ClientMessage.Reconnect(roomCode, myPlayerId, reconnectToken))
            }
        }
    }

    private suspend fun connectAndSend(firstMessage: ClientMessage) {
        // Pre-connection internet check
        if (!NetworkMonitor.isNetworkAvailable.value) {
            log.w { "No internet connection — aborting connect" }
            _errors.emit("No internet connection")
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }

        disconnect()
        shouldAutoReconnect = true
        _connectionState.value = ConnectionState.CONNECTING
        log.i { "Connecting to $serverUrl" }

        connectionJob = scope.launch {
            try {
                client.webSocket(urlString = "$serverUrl/game?v=${Protocol.VERSION}") {
                    webSocketSession = this
                    _connectionState.value = ConnectionState.CONNECTED
                    // NOTE: reconnectAttempts is reset on room ADMISSION (the first decoded message,
                    // in handleServerMessage) — NOT here. A protocol-rejection or rate-limit close
                    // lands right after this handshake succeeds, so resetting on connect kept the
                    // backoff pinned near zero and the give-up cap never tripped: the ~1Hz storm.
                    log.i { "WebSocket connected" }

                    // Send the initial message
                    val text = json.encodeToString(firstMessage)
                    send(Frame.Text(text))

                    // Listen for messages
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            handleServerMessage(frame.readText())
                        }
                    }

                    // Incoming closed → the server hung up. A CANNOT_ACCEPT (1003) close is the
                    // version gate: this build is too old and would be rejected on every retry,
                    // so make it terminal instead of auto-reconnecting into a storm.
                    if (closeReason.await()?.code == CloseReason.Codes.CANNOT_ACCEPT.code) {
                        shouldAutoReconnect = false
                        _fatalError.value = FatalSessionError.UPDATE_REQUIRED
                        OnlineSessionBackup.clear()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) { // Throwable, not Exception — see warmUp
                rethrowIfPlatformFatal(e)
                log.e(e) { "Connection error" }
                // Surface to the user only on the INITIAL connect (lobby create/join),
                // i.e. before the server has admitted us to a room. Once a session is
                // established (roomCode + myPlayerId set), a failure here is a transient
                // drop handled by auto-reconnect + the "Reconnecting…" banner; spamming
                // raw socket errors then is just noise. Terminal give-up is still reported
                // via "Failed to reconnect after N attempts" in scheduleReconnect().
                if (myPlayerId.isEmpty() || roomCode.isEmpty()) {
                    _errors.emit("Connection error: ${e.message}")
                }
            } finally {
                webSocketSession = null
                if (shouldAutoReconnect && roomCode.isNotEmpty() && myPlayerId.isNotEmpty()) {
                    _connectionState.value = ConnectionState.RECONNECTING
                    scheduleReconnect()
                } else {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        }
    }

    /**
     * Schedules a single reconnect attempt with exponential backoff + jitter.
     * Called from the connection's finally block on every failure, so the
     * attempt counter must live at repository level — a local variable here
     * would reset to the initial delay on each failure (the old bug that
     * caused 1 reconnect/second storms and rate-limit lockouts).
     */
    private fun scheduleReconnect() {
        autoReconnectJob?.cancel()
        autoReconnectJob = scope.launch {
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                _connectionState.value = ConnectionState.DISCONNECTED
                log.e { "Failed to reconnect after $MAX_RECONNECT_ATTEMPTS attempts" }
                _errors.emit("Failed to reconnect after $MAX_RECONNECT_ATTEMPTS attempts")
                reconnectAttempts = 0
                return@launch
            }

            // 1s, 2s, 4s, 8s, then 16s — jittered to 50–100% so players on a
            // shared network don't reconnect in lockstep after a WiFi blip.
            val base = (INITIAL_RECONNECT_DELAY_MS shl reconnectAttempts.coerceAtMost(4))
                .coerceAtMost(MAX_RECONNECT_DELAY_MS)
            val jittered = (base * (0.5 + Random.nextDouble() * 0.5)).toLong()
            delay(jittered)

            if (!shouldAutoReconnect) return@launch
            if (!NetworkMonitor.isNetworkAvailable.value) {
                // Offline: don't burn an attempt — the NetworkMonitor collector
                // in init reconnects immediately when the network returns.
                return@launch
            }

            reconnectAttempts++
            needsEventReplay.value = true
            connectAndSend(ClientMessage.Reconnect(roomCode, myPlayerId, reconnectToken))
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
        } catch (e: Throwable) { // Throwable, not Exception — see warmUp
            rethrowIfPlatformFatal(e)
            _errors.emit("Send failed: ${e.message}")
        }
    }

    private suspend fun handleServerMessage(text: String) {
        val message = try {
            json.decodeFromString<ServerMessage>(text)
        } catch (e: Exception) {
            log.e(e) { "Failed to decode server message: $text" }
            _errors.emit("Invalid server message")
            return
        }

        // Receiving a decoded server message proves the socket was admitted into the room flow
        // (a rejected client is closed at the handshake and never gets this far). This is the
        // correct place to reset the reconnect backoff — see the note at the handshake above.
        reconnectAttempts = 0
        OnlineSessionBackup.touch()

        when (message) {
            is ServerMessage.RoomCreated -> {
                roomCode = message.roomCode
                myPlayerId = message.playerId
                reconnectToken = message.reconnectToken
                OnlineSessionBackup.save(roomCode, myPlayerId, reconnectToken)
                log.i { "Room created: code=$roomCode, playerId=$myPlayerId" }
                if (message.protocolVersion > Protocol.VERSION) {
                    log.w { "Server protocol ${message.protocolVersion} is newer than client ${Protocol.VERSION} — app update recommended" }
                }
            }
            is ServerMessage.RoomUpdate -> {
                // A resume that lands in the LOBBY has no intro to suppress: disarm the flag,
                // or the next freshly started game would silently skip its match ceremony.
                // (A mid-game resume delivers GameUpdate before any RoomUpdate, so the flag
                // is already consumed by applyGameView when a game is actually running.)
                if (markNextGameAsResumed && _gameState.value == null) {
                    markNextGameAsResumed = false
                }
                _roomState.value = message.room
            }
            is ServerMessage.GameStarted -> {
                log.i { "Game started in room $roomCode" }
                applyGameView(message.view)
            }
            is ServerMessage.GameUpdate -> {
                applyGameView(message.view)
            }
            is ServerMessage.GameEventOccurred -> {
                // Broadcast copy of an event already spliced from the FINISHED view —
                // it's in the log and the strip in the right order; drop the duplicate.
                // Value match anywhere in the queue (NOT head order): the splice may also
                // sweep never-broadcast events (game start, early-end GameEnded) that no
                // broadcast copy will ever consume.
                if (pendingSplicedEvents.remove(message.event)) {
                    return
                }
                _eventLog = _eventLog + message.event
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
                log.w { "Server error: ${message.message}" }
                // "Room not found" while we hold a session (roomCode set) means the room is gone —
                // typically the server restarted and dropped all in-memory rooms. The socket stays
                // open, so without this the player is stranded on a frozen board under a green
                // "Reconnected" banner. Make it terminal so the UI can leave. An empty roomCode is a
                // failed lobby join, which the lobby surfaces as an ordinary error instead.
                if (roomCode.isNotEmpty() && message.message.contains("Room not found", ignoreCase = true)) {
                    _fatalError.value = FatalSessionError.ROOM_GONE
                    OnlineSessionBackup.clear()
                    disconnect()
                } else {
                    // A rejected reconnect ("Player not found" = seat gone, "Session invalid" =
                    // bad token) means the snapshot can never resume — clear it, or every web
                    // page load retries the doomed resume and swallows any fresh ?room= invite.
                    if (message.message.contains("Player not found", ignoreCase = true) ||
                        message.message.contains("Session invalid", ignoreCase = true)
                    ) {
                        OnlineSessionBackup.clear()
                    }
                    _errors.emit(message.message)
                }
            }
            is ServerMessage.RoomClosed -> {
                log.i { "Room $roomCode closed" }
                _errors.emit("Room was closed")
                disconnect()
            }
            is ServerMessage.HostTransferred -> {
                _playerEvents.emit(
                    PlayerConnectionEvent.HostChanged(message.newHostName)
                )
            }
            is ServerMessage.ReactionReceived -> {
                _reactions.emit(message)
            }
            is ServerMessage.RematchStarted -> {
                log.i { "Rematch started for room ${message.room.roomCode}" }
                // Clear game state BEFORE updating the room: the waiting room
                // auto-navigates to the game when gameState is non-null, so a
                // stale FINISHED state would bounce players straight back out.
                _gameState.value = null
                pendingSplicedEvents.clear()
                _reconnectCountdowns.value = emptyMap()
                lastSeenEventTimestamp = 0L
                needsEventReplay.value = false
                _roomState.value = message.room
                _rematchStarted.tryEmit(Unit)
            }
        }
    }

    private suspend fun applyGameView(view: PlayerGameView) {
        if (markNextGameAsResumed) {
            resumedGameId = view.gameId
            markNextGameAsResumed = false
        }
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
            // Server-issued per-match id (unique across rematches in the same room); fall
            // back to the room code only if talking to an older server that omits it.
            gameId = view.gameId.ifBlank { "online_${roomCode}" },
            players = players,
            teams = view.teams,
            currentPlayerIndex = currentPlayerIndex,
            phase = view.phase,
            halfSuitStatuses = view.halfSuitStatuses,
            events = view.recentEvents,
            playerCount = players.size,
            // Carry the server's Option C suspension through so the ViewModel/UI
            // can show the picker (or, for everyone else, a "choosing…" state).
            pendingPass = view.pendingPass,
            pendingPassDeadlineMs = view.pendingPassDeadlineMs
        )

        _gameState.value = syntheticState

        // A view for a new match starts a fresh accumulated log. The first GameUpdate
        // of a match arrives before its turn events, so nothing is lost to the reset.
        if (syntheticState.gameId != eventLogGameId) {
            eventLogGameId = syntheticState.gameId
            _eventLog = emptyList()
        }

        // On reconnect, replay events we missed while disconnected into _gameEvents
        // so the ViewModel's gameLog catches up. Only do this once after reconnecting,
        // not on every GameUpdate (which would duplicate events already arriving via
        // GameEventOccurred). compareAndSet atomically reads and clears the flag,
        // preventing a concurrent reconnect from being lost.
        if (needsEventReplay.compareAndSet(expect = true, update = false)) {
            val missedEvents = view.recentEvents.filter { it.timestamp > lastSeenEventTimestamp }
            _eventLog = _eventLog + missedEvents
            for (event in missedEvents) {
                _gameEvents.emit(event)
            }
            if (view.recentEvents.isNotEmpty()) {
                lastSeenEventTimestamp = view.recentEvents.maxOf { it.timestamp }
            }
        }

        // Game-end splice. The server broadcasts each move's VIEW before its EVENTS, so
        // the FINISHED view outruns the final DeckClaimed/TurnChanged/GameEnded frames —
        // and the engine's early-end path (a team runs out of cards -> bonus award, the
        // way most real games finish) never broadcasts its GameEnded at all. Splicing
        // only GameEnded here put "Game Over" BEFORE the final claim in the log and the
        // board strip. Instead, splice ALL not-yet-seen events from the view in engine
        // order, and remember them so the duplicate broadcast copies arriving right
        // behind this view are dropped (see GameEventOccurred).
        if (view.phase == GamePhase.FINISHED && _eventLog.none { it is GameEvent.GameEnded }) {
            val missed = view.recentEvents.filter { it.timestamp > lastSeenEventTimestamp }
            if (missed.isNotEmpty()) {
                _eventLog = _eventLog + missed
                for (event in missed) {
                    _gameEvents.emit(event)
                }
                lastSeenEventTimestamp = missed.maxOf { it.timestamp }
                pendingSplicedEvents.addAll(missed)
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
        _gameState.value = null
        // Do NOT cancel scope — it is application-lifetime (singleton). Cancelling it
        // permanently breaks connectAndSend for any subsequent online session.
    }
}

/** A terminal online-session condition the UI surfaces and then leaves on — reconnecting can't fix it. */
enum class FatalSessionError {
    /** The room is gone (e.g. the server restarted and lost its in-memory rooms). */
    ROOM_GONE,
    /** This client is too old for the server (protocol below MIN_SUPPORTED). */
    UPDATE_REQUIRED
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}
