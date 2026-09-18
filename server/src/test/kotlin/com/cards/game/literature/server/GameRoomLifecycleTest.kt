package com.cards.game.literature.server

import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.GameState
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.Player
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.Team
import com.cards.game.literature.protocol.RoomPhase
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Turn and room lifecycle guards, each a regression test for a failure seen or traced in
 * production: timers killed by off-turn moves, ask-ended games never marked finished, joins
 * into a running game, ghost seats after reconnect-then-drop, result-screen dropouts lost on
 * Rematch, reconnects into a finished room, host stuck on a departed player, bot decisions
 * that throw, and unequal teams.
 */
class GameRoomLifecycleTest {

    private val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
    private val heartsTwo = Card(Suit.HEARTS, CardValue.TWO)
    private val clubTwo = Card(Suit.CLUBS, CardValue.TWO)

    /**
     * Four seated humans, player_0 (t1) on turn holding two low spades; player_1 (t2) holds
     * the third. With [t2HasSpare] false a successful ask for it empties t2 and ends the game.
     */
    private fun runningRoom(t2HasSpare: Boolean): GameRoom {
        val room = GameRoom("TEST01", 4)
        repeat(4) { room.addPlayer("P$it", isHost = it == 0) }
        val players = listOf(
            Player("player_0", "P0", "t1", listOf(spadesLow[0], spadesLow[1])),
            Player("player_1", "P1", "t2", listOf(spadesLow[2])),
            Player("player_2", "P2", "t1", listOf(heartsTwo)),
            Player("player_3", "P3", "t2", if (t2HasSpare) listOf(clubTwo) else emptyList())
        )
        val teams = listOf(
            Team("t1", "Team 1", listOf("player_0", "player_2")),
            Team("t2", "Team 2", listOf("player_1", "player_3"))
        )
        room.installRunningGameForTest(
            GameState(
                gameId = "lifecycle-test",
                players = players,
                teams = teams,
                currentPlayerIndex = 0,
                phase = GamePhase.IN_PROGRESS,
                playerCount = 4
            )
        )
        return room
    }

    /** Fails if [from] is still on turn after 2 s — i.e. the turn timer never fired. With a
     *  shrunken clock the turn keeps rotating, so poll for the first change rather than sample
     *  the board at a fixed instant. */
    private suspend fun awaitTurnLeaves(room: GameRoom, from: String) {
        withTimeout(2_000) { while (room.currentPlayerId == from) delay(10) }
    }

    // --- F-11: the turn clock must survive off-turn and rejected moves ---

    @Test
    fun offTurnAskDoesNotCancelTheActivePlayersTimer() = runBlocking {
        val room = runningRoom(t2HasSpare = true)
        room.turnTimeoutMs = 200
        room.startTurnTimerForTest()

        // player_1 is not on turn; before the fix this cancelled player_0's clock for good.
        room.processAsk("player_1", "player_0", listOf(spadesLow[0]))
        assertEquals("player_0", room.currentPlayerId, "an off-turn ask must not move the turn")

        awaitTurnLeaves(room, "player_0") // player_0's clock must still fire
        room.cleanup()
    }

    @Test
    fun rejectedOnTurnAskReArmsTheTimer() = runBlocking {
        val room = runningRoom(t2HasSpare = true)
        room.turnTimeoutMs = 200
        room.startTurnTimerForTest()

        // player_0 holds no hearts, so the engine rejects this with IllegalArgumentException.
        runCatching { room.processAsk("player_0", "player_1", listOf(heartsTwo)) }
        assertEquals("player_0", room.currentPlayerId)

        awaitTurnLeaves(room, "player_0") // the clock must be re-armed after a rejected move
        room.cleanup()
    }

    // --- F-12: a game that ends by an ask must finish the room ---

    @Test
    fun askEndingTheGameMarksRoomFinishedAndAllowsRematch() = runBlocking {
        val room = runningRoom(t2HasSpare = false)

        room.processAsk("player_0", "player_1", listOf(spadesLow[2])) // takes t2's last card

        assertEquals(RoomPhase.FINISHED, room.phase)
        assertTrue(room.finishedAt > 0L)
        assertTrue(room.resetForRematch(), "Rematch must be accepted after an ask-ending")
        assertEquals(RoomPhase.WAITING, room.phase)
        room.cleanup()
    }

    // --- F-13: no joining a running game ---

    @Test
    fun roomIsNotJoinableOnceTheGameStarted() = runBlocking {
        val waiting = GameRoom("TEST02", 4)
        waiting.addPlayer("Host", isHost = true)
        assertTrue(waiting.isJoinable())

        val running = runningRoom(t2HasSpare = true)
        assertFalse(running.isJoinable())
        running.cleanup()
    }

    // --- F-14: reconnect, then drop again, must not revive a ghost human seat ---

    @Test
    fun reconnectThenDropClearsThePendingReclaim() = runBlocking {
        val room = runningRoom(t2HasSpare = true)

        room.handleIntentionalLeave("player_1")          // seat becomes a bot at once
        assertTrue(room.handleReconnect("player_1"))     // queued to reclaim at their turn
        assertTrue("player_1" in room.pendingReclaimIdsForTest)

        room.handleDisconnect("player_1")                // ...but they drop again
        assertFalse("player_1" in room.pendingReclaimIdsForTest, "a dropped player must not be reclaimed")
        room.cleanup()
    }

    // --- F-15: result-screen dropouts keep their seat through a Rematch ---

    @Test
    fun finishedPhaseDisconnectKeepsSeatThroughRematch() = runBlocking {
        val room = runningRoom(t2HasSpare = false)
        room.processAsk("player_0", "player_1", listOf(spadesLow[2]))
        assertEquals(RoomPhase.FINISHED, room.phase)

        room.handleDisconnect("player_2")                // glanced at WhatsApp on the result screen
        assertNotNull(room.getPlayerSession("player_2")!!.disconnectDeadline, "finished-phase drop gets a grace window")
        assertFalse(room.isAbandoned(), "one dropout must not make the room abandoned")

        // player_3 dropped too, but long ago: their window is over.
        room.handleDisconnect("player_3")
        room.getPlayerSession("player_3")!!.disconnectDeadline = System.currentTimeMillis() - 1

        assertTrue(room.resetForRematch())
        assertNotNull(room.getPlayerSession("player_2"), "in-window dropout keeps the seat")
        assertNull(room.getPlayerSession("player_3"), "expired dropout is removed")
        room.cleanup()
    }

    @Test
    fun disconnectedSeatStartsAsABotAndIsQueuedForReclaimOnReturn() = runBlocking {
        val room = GameRoom("TEST05", 4)
        room.addPlayer("Host", isHost = true)
        val guest = room.addPlayer("Guest")
        room.getPlayerSession(guest)!!.isConnected = false   // kept through a rematch grace window

        val hostSocket = RecordingSocket()
        room.getPlayerSession("player_0")!!.session = hostSocket

        assertTrue(room.startGame(fillWithBots = true))
        assertEquals(true, room.isBotSeatForTest(guest), "an absent owner's seat is played by a bot")
        assertTrue(hostSocket.sentTypes().any { it.endsWith("GameEventOccurred") } &&
            hostSocket.sentText().contains("PlayerReplacedByBot"),
            "the table is told the absent seat is a bot")

        room.handleReconnect(guest)
        // The bot loop runs on its own thread: depending on scheduling it may already have
        // performed the reclaim (seat back to human) or not yet (seat queued). Both are the
        // owner getting the seat back; asserting only "queued" was a CI-only race.
        assertTrue(
            guest in room.pendingReclaimIdsForTest || room.isBotSeatForTest(guest) == false,
            "the owner gets the seat back (queued for their next turn, or already reclaimed)"
        )
        room.cleanup()
    }

    @Test
    fun seatKeptThroughRematchGetsRematchStartedOnReconnectEvenMidGame() = runBlocking {
        val room = runningRoom(t2HasSpare = false)
        room.processAsk("player_0", "player_1", listOf(spadesLow[2]))   // game ends
        room.handleDisconnect("player_2")                                // drops on the result screen
        assertTrue(room.resetForRematch())                               // kept: still in its window
        assertTrue(room.startGame(fillWithBots = true))                  // host starts without them

        val socket = RecordingSocket()
        room.getPlayerSession("player_2")!!.session = socket
        room.getPlayerSession("player_2")!!.protocolVersion = 3
        room.handleReconnect("player_2")

        val types = socket.sentTypes()
        val rematchAt = types.indexOfFirst { it.endsWith("RematchStarted") }
        val viewAt = types.indexOfFirst { it.endsWith("GameUpdate") }
        assertTrue(rematchAt >= 0, "missed RematchStarted must be re-sent, got $types")
        assertTrue(viewAt > rematchAt, "the game view must follow the rematch signal, got $types")
        room.cleanup()
    }

    @Test
    fun hostDroppingOnResultScreenHandsHostOnAfterTheWindow() = runBlocking {
        val room = runningRoom(t2HasSpare = false)
        room.processAsk("player_0", "player_1", listOf(spadesLow[2]))   // game ends; player_0 is host
        room.reconnectWindowMs = 150

        room.handleDisconnect("player_0")                                // host's socket dies on the result screen
        assertTrue(room.isHost("player_0"), "host is kept during the grace window")

        withTimeout(2_000) { while (room.isHost("player_0")) delay(20) }
        val newHost = room.toRoomState().hostPlayerId
        assertTrue(room.getPlayerSession(newHost)!!.isConnected, "host moves to a connected player")
        room.cleanup()
    }

    // --- F-05: reconnecting into a finished room delivers the game view ---

    @Test
    fun reconnectIntoFinishedRoomSendsTheGameView() = runBlocking {
        val room = runningRoom(t2HasSpare = false)
        room.processAsk("player_0", "player_1", listOf(spadesLow[2]))
        assertEquals(RoomPhase.FINISHED, room.phase)

        room.handleDisconnect("player_2")
        val socket = RecordingSocket()
        room.getPlayerSession("player_2")!!.session = socket
        room.handleReconnect("player_2")

        val types = socket.sentTypes()
        assertTrue(types.any { it.endsWith("GameUpdate") }, "expected a GameUpdate, got $types")
        room.cleanup()
    }

    // --- F-26 (server half): host follows the players who are still here ---

    @Test
    fun hostLeavingMidGameTransfersHostToAConnectedPlayer() = runBlocking {
        val room = runningRoom(t2HasSpare = true)
        assertTrue(room.isHost("player_0"))

        room.handleIntentionalLeave("player_0")

        assertFalse(room.isHost("player_0"))
        val newHost = room.toRoomState().hostPlayerId
        assertTrue(newHost in setOf("player_1", "player_2", "player_3"), "host must move to a seated player")
        assertTrue(room.getPlayerSession(newHost)!!.isConnected)
        room.cleanup()
    }

    // --- F-21: a throwing bot decision must not wedge the table ---

    @Test
    fun throwingBotDecisionSkipsItsTurnInsteadOfFreezing() = runBlocking {
        val room = GameRoom("TEST03", 4)
        val humans = listOf(room.addPlayer("Alice", isHost = true), room.addPlayer("Bob"))
        room.decideBotMove = { _, _ -> throw NoSuchElementException("List is empty.") }

        withTimeout(10_000) {
            assertTrue(room.startGame(fillWithBots = true))
            // Put a bot on turn if a human opened.
            val opener = room.currentPlayerId!!
            if (opener in humans) room.handleIntentionalLeave(opener)
            val remainingHuman = humans.first { it != opener }
            // The backstop gives up the bot's turn after MAX failures; the turn must reach the human.
            while (room.currentPlayerId != remainingHuman && room.phase == RoomPhase.IN_PROGRESS) delay(50)
            assertEquals(remainingHuman, room.currentPlayerId)
        }
        room.cleanup()
    }

    // --- F-46: two equal teams or no game ---

    @Test
    fun unequalHumanTeamsRefuseToStart() = runBlocking {
        val room = GameRoom("TEST04", 4)
        repeat(4) { room.addPlayer("P$it", isHost = it == 0) }
        room.switchTeam("player_1") // team_1 now has three, team_2 one

        assertFalse(room.startGame(fillWithBots = false))
        assertEquals(RoomPhase.WAITING, room.phase)

        room.switchTeam("player_1")
        assertTrue(room.startGame(fillWithBots = false))
        room.cleanup()
    }

    /** Minimal WebSocketSession that records outgoing frames so tests can see what a player got. */
    private class RecordingSocket : WebSocketSession {
        private val out = Channel<Frame>(Channel.UNLIMITED)
        override val coroutineContext: CoroutineContext = Dispatchers.Default
        override val incoming: ReceiveChannel<Frame> = Channel()
        override val outgoing: SendChannel<Frame> = out
        override val extensions: List<WebSocketExtension<*>> = emptyList()
        override var maxFrameSize: Long = Long.MAX_VALUE
        override var masking: Boolean = false
        override suspend fun flush() {}
        @Deprecated("Use cancel() instead.", level = DeprecationLevel.ERROR)
        override fun terminate() {}

        private val seen = mutableListOf<String>()

        /** Every text frame received so far (drained frames are remembered). */
        fun sentText(): String = seen.joinToString("\n")

        fun sentTypes(): List<String> = buildList {
            while (true) {
                val frame = out.tryReceive().getOrNull() ?: break
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    seen += text
                    Regex("\"type\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)?.let { add(it) }
                }
            }
        }
    }
}
