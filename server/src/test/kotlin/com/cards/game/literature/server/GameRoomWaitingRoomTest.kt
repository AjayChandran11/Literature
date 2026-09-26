package com.cards.game.literature.server

import com.cards.game.literature.protocol.RoomPhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Assembling a group in the waiting room. Production logs (2026-09-21) showed a 6-player room
 * issue SEVEN seats in 13 minutes and never start a game: people left the app to paste the code
 * into WhatsApp, Android killed their sockets, and the 2-minute window removed their seats — so
 * each of them came back as a new player on a possibly different team. These tests pin the three
 * rules that fix it: seats are held for longer, a returning player gets their own seat back, and
 * a held seat never blocks someone who is actually here.
 */
class GameRoomWaitingRoomTest {

    private fun waitingRoom(target: Int, names: List<String>): GameRoom {
        val room = GameRoom("TESTWR", target)
        names.forEachIndexed { i, n -> room.addPlayer(n, isHost = i == 0) }
        return room
    }

    /** Marks a seat as away [secondsAgo] seconds ago, without scheduling its removal. */
    private fun GameRoom.markAway(playerId: String, secondsAgo: Long) {
        getPlayerSession(playerId)!!.let {
            it.isConnected = false
            it.lastSeen = System.currentTimeMillis() - secondsAgo * 1000
        }
    }

    private fun GameRoom.teamOf(playerId: String): String =
        toRoomState().players.first { it.id == playerId }.teamId

    @Test
    fun aReturningPlayerGetsTheirOwnSeatAndTeamBack() = runBlocking {
        val room = waitingRoom(6, listOf("GA", "Vinu", "prem"))
        val seat = "player_1" // Vinu
        val team = room.teamOf(seat)
        room.handleDisconnect(seat) // held, not removed: the window is 5 minutes

        val reclaimed = room.findSeatToReclaim("Vinu")
        assertEquals(seat, reclaimed, "the same name should get the held seat back")
        room.reclaimSeat(reclaimed!!)

        assertTrue(room.getPlayerSession(seat)!!.isConnected)
        assertNull(room.getPlayerSession(seat)!!.disconnectDeadline, "the pending removal is cancelled")
        assertEquals(team, room.teamOf(seat), "their team survives the round trip")
        assertEquals(3, room.getHumanPlayerCount(), "no extra seat is minted")
        room.cleanup()
    }

    @Test
    fun theNameMatchIsCaseAndSpaceInsensitiveAndOnlyTakesAwaySeats() = runBlocking {
        val room = waitingRoom(6, listOf("GA", "Vinu"))
        room.markAway("player_1", secondsAgo = 30)

        assertEquals("player_1", room.findSeatToReclaim("  vinu "))
        assertNull(room.findSeatToReclaim("Someone else"))
        assertNull(room.findSeatToReclaim("GA"), "a seat whose owner is present is not up for grabs")
        room.cleanup()
    }

    @Test
    fun theMostRecentSeatIsReturnedWhenAPlayerLeftSeveralBehind() = runBlocking {
        val room = waitingRoom(6, listOf("GA", "prem", "prem"))
        room.markAway("player_1", secondsAgo = 300)
        room.markAway("player_2", secondsAgo = 20)

        assertEquals("player_2", room.findSeatToReclaim("prem"), "their latest seat, not their oldest")
        room.cleanup()
    }

    @Test
    fun onceTheWindowPassesTheSeatIsGoneAndTheyJoinAsSomeoneNew() = runBlocking {
        val room = waitingRoom(6, listOf("GA", "Vinu"))
        room.waitingWindowMs = 100
        room.handleDisconnect("player_1")

        withTimeout(2_000) { while (room.getPlayerSession("player_1") != null) delay(20) }
        assertNull(room.findSeatToReclaim("Vinu"), "nothing left to reclaim")

        val fresh = room.addPlayer("Vinu")
        assertNotEquals("player_1", fresh)
        room.cleanup()
    }

    @Test
    fun aFullRoomFreesTheLongestAbsentSeatForAnArrivingPlayer() = runBlocking {
        val room = waitingRoom(4, listOf("GA", "Vinu", "prem", "SP"))
        room.markAway("player_1", secondsAgo = 120) // away two minutes
        room.markAway("player_3", secondsAgo = 240) // away four minutes — the one to give up

        assertTrue(room.isJoinable(), "full of held seats still admits someone who is here")
        assertTrue(room.evictLongestAbsentSeat())

        assertNull(room.getPlayerSession("player_3"), "the longest-absent seat is the one freed")
        assertEquals("player_1", room.findSeatToReclaim("Vinu"), "the recently-away seat is untouched")
        assertEquals(3, room.getHumanPlayerCount())
        room.cleanup()
    }

    @Test
    fun aSeatThatJustDroppedIsNotGivenAway() = runBlocking {
        val room = waitingRoom(4, listOf("GA", "Vinu", "prem", "SP"))
        room.markAway("player_2", secondsAgo = 5) // blinked out a moment ago

        assertFalse(room.evictLongestAbsentSeat(), "a momentary drop keeps the seat")
        assertFalse(room.isJoinable())
        assertEquals("player_2", room.findSeatToReclaim("prem"), "and they can still reclaim it")
        room.cleanup()
    }

    @Test
    fun aFullRoomOfPresentPlayersStaysFull() = runBlocking {
        val room = waitingRoom(4, listOf("GA", "Vinu", "prem", "SP"))

        assertFalse(room.evictLongestAbsentSeat(), "nobody is away, so nobody is dropped")
        assertFalse(room.isJoinable())
        assertEquals(4, room.getHumanPlayerCount())
        room.cleanup()
    }

    @Test
    fun evictingTheHostPassesHostToSomeoneWhoIsHere() = runBlocking {
        val room = waitingRoom(2, listOf("GA", "Vinu"))
        assertTrue(room.isHost("player_0"))
        room.markAway("player_0", secondsAgo = 300)

        assertTrue(room.evictLongestAbsentSeat())

        assertNull(room.getPlayerSession("player_0"))
        assertTrue(room.isHost("player_1"), "the room is never left without a host")
        room.cleanup()
    }

    @Test
    fun aSeatIsOnlyReclaimableWhileTheRoomIsWaiting() = runBlocking {
        val room = waitingRoom(4, listOf("GA", "Vinu"))
        room.markAway("player_1", secondsAgo = 30)
        assertTrue(room.startGame(fillWithBots = true))
        assertEquals(RoomPhase.IN_PROGRESS, room.phase)

        assertNull(room.findSeatToReclaim("Vinu"), "a running game seats nobody new")
        assertFalse(room.isJoinable())
        room.cleanup()
    }
}
