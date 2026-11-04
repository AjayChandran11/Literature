package com.cards.game.literature.server

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
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.withTimeout

/**
 * Tables: the rules that let strangers and late friends sit down without spoiling anyone's game.
 * Quick Play only ever seats people at a table that has not dealt; a code or invite link can
 * put a friend into a running game, but only in a bot's seat, never one a player is holding.
 */
class GameRoomTablesTest {

    private fun seated(room: GameRoom, name: String, version: Int = 4): Pair<String, RecordingSocket> {
        val id = room.addPlayer(name, isHost = room.getHumanPlayerCount() == 0)
        val socket = RecordingSocket()
        room.getPlayerSession(id)!!.let { it.session = socket; it.protocolVersion = version }
        return id to socket
    }

    private fun GameRoom.teamOf(playerId: String): String =
        toRoomState().players.first { it.id == playerId }.teamId

    @Test
    fun aLateFriendTakesABotSeatOnTheTeamWithFewerPeople() = runBlocking {
        val room = GameRoom("TBL1", 6)
        val (ga, gaSocket) = seated(room, "GA", version = 4)
        val (vinu, vinuSocket) = seated(room, "Vinu", version = 3)
        room.switchTeam(vinu) // both humans on team_1; team_2 is all bots
        assertTrue(room.startGame(fillWithBots = true))
        gaSocket.sentTypes(); vinuSocket.sentTypes()

        val taken = room.claimSeatInGame("Mala")
        assertIs<GameRoom.SeatResult.Seated>(taken)
        val seat = taken.playerId
        assertEquals("team_2", room.teamOf(seat), "the empty team gets the newcomer")
        assertEquals("Mala", room.getPlayerSession(seat)!!.playerName)

        val malaSocket = RecordingSocket()
        room.getPlayerSession(seat)!!.let { it.session = malaSocket; it.protocolVersion = 4 }
        room.completeSeatInGame(seat)

        assertEquals(false, room.isBotSeatForTest(seat), "the seat is human now (it was not on turn)")
        assertTrue(malaSocket.sentTypes().any { it.endsWith("GameUpdate") }, "the newcomer gets the board")
        assertTrue(gaSocket.sentText().contains("PlayerJoined"), "a v4 client hears who arrived")
        val vinuSaw = vinuSocket.sentText()
        assertTrue(vinuSaw.contains("PlayerReconnected") && !vinuSaw.contains("PlayerJoined"),
            "a v3 client gets the nearest event it can decode")
        assertEquals(ga, room.toRoomState().hostPlayerId)
        room.cleanup()
    }

    @Test
    fun aHeldSeatIsKeptForItsOwnerUntilTheHoldExpires() = runBlocking {
        val room = GameRoom("TBL2", 4)
        val (_, _) = seated(room, "GA")
        val (vinu, _) = seated(room, "Vinu")
        room.reconnectWindowMs = 30
        assertTrue(room.startGame(fillWithBots = true))

        room.handleDisconnect(vinu)
        delay(400)
        assertEquals(true, room.isBotSeatForTest(vinu), "a bot stands in for Vinu after the window")

        val first = room.claimSeatInGame("Mala")
        val second = room.claimSeatInGame("Arun")
        assertIs<GameRoom.SeatResult.Seated>(first)
        assertIs<GameRoom.SeatResult.Seated>(second)
        assertNotEquals(vinu, first.playerId); assertNotEquals(vinu, second.playerId)
        assertIs<GameRoom.SeatResult.NoSeat>(room.claimSeatInGame("Dev"), "Vinu's seat is on hold")

        val later = System.currentTimeMillis() + room.takeoverAfterMs + 1_000
        val third = room.claimSeatInGame("Dev", now = later)
        assertIs<GameRoom.SeatResult.Seated>(third)
        assertEquals(vinu, third.playerId, "past the hold, the seat is given away")
        assertEquals("Dev", room.getPlayerSession(vinu)!!.playerName, "and Vinu's record is gone with it")
        room.cleanup()
    }

    @Test
    fun aTableThatHasNotDealtOrHasFinishedGivesNoSeatThisWay() = runBlocking {
        val room = GameRoom("TBL3", 4)
        seated(room, "GA")
        assertIs<GameRoom.SeatResult.NotWaiting>(room.claimSeatInGame("Mala"), "waiting rooms use trySeat")
        room.cleanup()
    }

    @Test
    fun aQuickTableDealsItselfUnlessSomeoneStartsItFirst() = runBlocking {
        val room = GameRoom("QP1", 4, isPublic = true)
        seated(room, "GA")
        room.scheduleAutoStart(150)
        assertNotNull(room.autoStartAtMs)
        delay(700)
        assertEquals(RoomPhase.IN_PROGRESS, room.phase, "dealt by itself, with bots")
        assertNull(room.autoStartAtMs)
        room.cleanup()

        val eager = GameRoom("QP2", 4, isPublic = true)
        seated(eager, "GA")
        eager.scheduleAutoStart(5_000)
        assertTrue(eager.startGame(fillWithBots = true))
        assertNull(eager.autoStartAtMs, "an explicit start cancels the countdown")
        delay(100)
        assertEquals(RoomPhase.IN_PROGRESS, eager.phase)
        eager.cleanup()
    }

    @Test
    fun anEmptyQuickTableDoesNotDeal() = runBlocking {
        val room = GameRoom("QP3", 4, isPublic = true)
        val (ga, _) = seated(room, "GA")
        room.getPlayerSession(ga)!!.isConnected = false
        room.scheduleAutoStart(100)
        delay(500)
        assertEquals(RoomPhase.WAITING, room.phase, "nobody is here to play")
        assertNull(room.autoStartAtMs)
        room.cleanup()
    }

    @Test
    fun aDeclinedAutoStartTellsTheTableAndKeepsTheClockRunning() = runBlocking {
        val room = GameRoom("QP4", 4, isPublic = true)
        val (_, aSocket) = seated(room, "A")
        val (b, _) = seated(room, "B")
        val (c, _) = seated(room, "C")
        room.switchTeam(b) // three on team_1: no bot can make that 2v2
        room.scheduleAutoStart(100)
        delay(450)
        assertEquals(RoomPhase.WAITING, room.phase, "a 3v1 table cannot deal")
        assertNotNull(room.autoStartAtMs, "but the clock is re-armed, never left off")
        assertTrue(aSocket.sentText().contains("Both teams must have the same number of players"), "and the table is told why")

        room.switchTeam(c)
        withTimeout(2_000) { while (room.phase == RoomPhase.WAITING) delay(20) }
        assertEquals(RoomPhase.IN_PROGRESS, room.phase, "even teams again: the next tick deals")
        room.cleanup()
    }

    @Test
    fun aRefusedStartLeavesTheCountdownAlone() = runBlocking {
        val room = GameRoom("QP5", 4, isPublic = true)
        seated(room, "A")
        room.scheduleAutoStart(5_000)
        assertFalse(room.startGame(fillWithBots = false), "one person, no bots: nothing to deal")
        assertNotNull(room.autoStartAtMs, "the table still deals itself later")
        room.cleanup()
    }

    @Test
    fun nobodyIsSeatedInTheRoomButNotInTheGame() = runBlocking {
        repeat(25) { round ->
            val room = GameRoom("RACE$round", 8)
            seated(room, "A"); seated(room, "B")
            val joins = java.util.concurrent.ConcurrentLinkedQueue<GameRoom.SeatResult>()
            val joiner = Thread {
                var i = 0
                while (true) {
                    val r = room.trySeat("J${i++}")
                    joins += r
                    if (r is GameRoom.SeatResult.NotWaiting) break
                }
            }
            joiner.start()
            assertTrue(room.startGame(fillWithBots = true))
            joiner.join(2_000)

            // Every seat the room handed out before the deal is in the game; after it, none.
            joins.filterIsInstance<GameRoom.SeatResult.Seated>().forEach { seat ->
                assertNotNull(room.isBotSeatForTest(seat.playerId), "round $round: ${seat.playerId} is on the roster but not in the game")
            }
            room.cleanup()
        }
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

        fun sentText(): String { sentTypes(); return seen.joinToString("\n") }

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
