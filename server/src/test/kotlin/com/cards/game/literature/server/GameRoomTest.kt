package com.cards.game.literature.server

import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.ClaimDeclaration
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.protocol.RoomPhase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameRoomTest {

    private fun room(targetPlayers: Int = 4) = GameRoom("TEST01", targetPlayers)

    @Test
    fun addPlayerAssignsSequentialIdsAndAlternatingTeams() {
        val room = room()
        val id0 = room.addPlayer("Alice", isHost = true)
        val id1 = room.addPlayer("Bob")
        val id2 = room.addPlayer("Carol")

        assertEquals("player_0", id0)
        assertEquals("player_1", id1)
        assertEquals("player_2", id2)
        assertTrue(room.isHost(id0))
        assertFalse(room.isHost(id1))

        val players = room.toRoomState().players
        assertEquals("team_1", players.first { it.id == id0 }.teamId)
        assertEquals("team_2", players.first { it.id == id1 }.teamId)
        assertEquals("team_1", players.first { it.id == id2 }.teamId)
    }

    @Test
    fun reconnectTokensAreUniqueAndNonEmpty() {
        val room = room()
        val id0 = room.addPlayer("Alice")
        val id1 = room.addPlayer("Bob")

        val token0 = room.getPlayerSession(id0)!!.reconnectToken
        val token1 = room.getPlayerSession(id1)!!.reconnectToken

        assertEquals(32, token0.length, "expected 128-bit hex token")
        assertEquals(32, token1.length)
        assertNotEquals(token0, token1)
    }

    @Test
    fun switchTeamToggles() {
        val room = room()
        val id = room.addPlayer("Alice")
        val before = room.toRoomState().players.first().teamId
        room.switchTeam(id)
        val after = room.toRoomState().players.first().teamId
        assertNotEquals(before, after)
    }

    @Test
    fun removePlayerTransfersHost() {
        val room = room()
        val host = room.addPlayer("Alice", isHost = true)
        val other = room.addPlayer("Bob")
        room.removePlayer(host)
        assertTrue(room.isHost(other))
    }

    @Test
    fun startGameFillsWithBotsAndStarts() = runBlocking {
        val room = room(targetPlayers = 4)
        room.addPlayer("Alice", isHost = true)

        val started = room.startGame(fillWithBots = true)

        assertTrue(started)
        assertEquals(RoomPhase.IN_PROGRESS, room.phase)
        assertEquals(1, room.getHumanPlayerCount())
        room.cleanup()
    }

    @Test
    fun startGameWithoutBotsRequiresFullRoom() = runBlocking {
        val room = room(targetPlayers = 4)
        room.addPlayer("Alice", isHost = true)

        val started = room.startGame(fillWithBots = false)

        assertFalse(started)
        assertEquals(RoomPhase.WAITING, room.phase)
    }

    @Test
    fun startGameTwiceFails() = runBlocking {
        val room = room(targetPlayers = 4)
        room.addPlayer("Alice", isHost = true)
        assertTrue(room.startGame(fillWithBots = true))
        assertFalse(room.startGame(fillWithBots = true), "game already in progress")
        room.cleanup()
    }

    // --- isAbandoned: the room-cleanup contract ---

    @Test
    fun roomWithConnectedPlayerIsNotAbandoned() {
        val room = room()
        room.addPlayer("Alice") // PlayerSession defaults to isConnected = true
        assertFalse(room.isAbandoned())
    }

    @Test
    fun allDisconnectedWithoutDeadlinesIsAbandoned() {
        val room = room()
        val id = room.addPlayer("Alice")
        room.getPlayerSession(id)!!.isConnected = false
        assertTrue(room.isAbandoned())
    }

    @Test
    fun pendingReconnectDeadlineKeepsRoomAlive() {
        val room = room()
        val id = room.addPlayer("Alice")
        val session = room.getPlayerSession(id)!!
        session.isConnected = false
        session.disconnectDeadline = System.currentTimeMillis() + 120_000

        assertFalse(
            room.isAbandoned(),
            "room must survive while a player is within their reconnect window"
        )
    }

    @Test
    fun expiredReconnectDeadlinePlusGraceIsAbandoned() {
        val room = room()
        val id = room.addPlayer("Alice")
        val session = room.getPlayerSession(id)!!
        session.isConnected = false
        // Past the deadline AND past the 60s grace buffer
        session.disconnectDeadline = System.currentTimeMillis() - 61_000

        assertTrue(room.isAbandoned())
    }

    @Test
    fun deadlineWithinGraceBufferKeepsRoomAlive() {
        val room = room()
        val id = room.addPlayer("Alice")
        val session = room.getPlayerSession(id)!!
        session.isConnected = false
        // Deadline just passed — still inside the grace buffer
        session.disconnectDeadline = System.currentTimeMillis() - 1_000

        assertFalse(room.isAbandoned())
    }

    @Test
    fun emptyRoomIsAbandoned() {
        assertTrue(room().isAbandoned())
    }

    @Test
    fun disconnectInWaitingRoomRemovesPlayer() = runBlocking {
        val room = room()
        room.addPlayer("Alice", isHost = true)
        val bob = room.addPlayer("Bob")

        room.handleDisconnect(bob)

        assertEquals(1, room.getHumanPlayerCount())
    }

    @Test
    fun rematchIsRejectedUnlessGameFinished() = runBlocking {
        val room = room()
        room.addPlayer("Alice", isHost = true)

        // WAITING — nothing to rematch
        assertFalse(room.resetForRematch())
        assertEquals(RoomPhase.WAITING, room.phase)

        // IN_PROGRESS — game must finish first
        room.startGame(fillWithBots = true)
        assertFalse(room.resetForRematch())
        assertEquals(RoomPhase.IN_PROGRESS, room.phase)
        room.cleanup()
    }

    /** Drives a real 4-human game to completion via claims, then rematches. */
    @Test
    fun rematchAfterFinishedGameResetsRoomKeepingConnectedPlayers() = runBlocking {
        val room = room(targetPlayers = 4)
        val alice = room.addPlayer("Alice", isHost = true)
        val bob = room.addPlayer("Bob")
        room.addPlayer("Carol")
        room.addPlayer("Dave")
        assertTrue(room.startGame(fillWithBots = false))
        assertEquals(RoomPhase.IN_PROGRESS, room.phase)

        // Whoever holds the turn claims the next unclaimed half-suit with all
        // six cards assigned to themselves. Wrong claims still remove the
        // cards and award a point, so the game must reach FINISHED (after 8
        // claims, or earlier if a team runs out of cards).
        for (halfSuit in HalfSuit.entries) {
            if (room.phase == RoomPhase.FINISHED) break
            val current = room.currentPlayerId ?: break
            room.processClaim(
                current,
                ClaimDeclaration(
                    claimerId = current,
                    halfSuit = halfSuit,
                    cardAssignments = mapOf(current to DeckUtils.getAllCardsForHalfSuit(halfSuit))
                )
            )
        }
        assertEquals(RoomPhase.FINISHED, room.phase)

        // Bob leaves before the rematch
        room.getPlayerSession(bob)!!.isConnected = false

        assertTrue(room.resetForRematch())
        assertEquals(RoomPhase.WAITING, room.phase)
        assertNull(room.currentPlayerId, "game state must be cleared")
        assertEquals(3, room.getHumanPlayerCount(), "disconnected player dropped")
        assertNull(room.getPlayerSession(bob))
        assertTrue(room.isHost(alice), "host retained")
        assertFalse(room.isAbandoned(), "room must not be cleanable after rematch")

        // And the fresh room can start another game
        assertTrue(room.startGame(fillWithBots = true))
        assertEquals(RoomPhase.IN_PROGRESS, room.phase)
        room.cleanup()
    }

    // --- intentional leave: ghost-session regression (production bug) ---
    // The LeaveGame handler nulls its IDs before closing the socket, so the
    // connection's finally-block bookkeeping never runs for a leaver. The
    // leave itself must mark the session gone, or the player stays
    // "connected" on a dead socket — blocking room cleanup forever.

    @Test
    fun intentionalLeaveMarksSessionGone() = runBlocking {
        val room = room()
        room.addPlayer("Alice", isHost = true)
        val bob = room.addPlayer("Bob")
        room.startGame(fillWithBots = true)

        room.handleIntentionalLeave(bob)

        val session = room.getPlayerSession(bob)!!
        assertFalse(session.isConnected, "leaver must not stay 'connected'")
        assertNull(session.session, "dead socket must not be retained")
        assertTrue(session.intentionalLeave)
        room.cleanup()
    }

    @Test
    fun roomIsAbandonedAfterEveryoneLeavesIntentionally() = runBlocking {
        val room = room()
        val alice = room.addPlayer("Alice", isHost = true)
        val bob = room.addPlayer("Bob")
        room.startGame(fillWithBots = true)

        room.handleIntentionalLeave(bob)
        room.handleIntentionalLeave(alice)

        // Leavers carry no reconnect deadline — the room must be cleanable
        assertTrue(
            room.isAbandoned(),
            "room with only intentional leavers must be sweepable (was leaked in production)"
        )
        room.cleanup()
    }
}
