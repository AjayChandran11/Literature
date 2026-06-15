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
import kotlin.test.assertNotNull
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

    // --- WAITING-room disconnect grace (invite/background regression) ---
    // Sharing an invite forces the host out of the app, dropping their socket. The
    // waiting room must HOLD the seat for a grace window (not remove instantly) so the
    // host reconnects into the same room and guests can still join it. Previously the
    // player was removed on the spot, which destroyed/orphaned the room.

    @Test
    fun disconnectInWaitingRoomKeepsSeatDuringGrace() = runBlocking {
        val room = room()
        room.addPlayer("Alice", isHost = true)
        val bob = room.addPlayer("Bob")

        room.handleDisconnect(bob)

        assertEquals(2, room.getHumanPlayerCount(), "seat held during grace window")
        val session = room.getPlayerSession(bob)!!
        assertFalse(session.isConnected, "marked disconnected")
        assertNotNull(session.disconnectDeadline, "reconnect deadline set")
        assertFalse(room.isAbandoned(), "room kept alive during the window")
        room.cleanup()
    }

    @Test
    fun reconnectInWaitingRoomRestoresSeat() = runBlocking {
        val room = room()
        room.addPlayer("Alice", isHost = true)
        val bob = room.addPlayer("Bob")

        room.handleDisconnect(bob)
        val reconnected = room.handleReconnect(bob)

        assertTrue(reconnected)
        val session = room.getPlayerSession(bob)!!
        assertTrue(session.isConnected, "seat restored on reconnect")
        assertNull(session.disconnectDeadline, "deadline cleared")
        assertEquals(2, room.getHumanPlayerCount())
        room.cleanup()
    }

    @Test
    fun waitingDisconnectFinalizeRemovesAfterGrace() = runBlocking {
        val room = room()
        room.addPlayer("Alice", isHost = true)
        val bob = room.addPlayer("Bob")

        room.handleDisconnect(bob)
        room.finalizeWaitingDisconnect(bob) // simulate the grace window elapsing

        assertEquals(1, room.getHumanPlayerCount(), "removed after grace with no reconnect")
        assertNull(room.getPlayerSession(bob))
        room.cleanup()
    }

    @Test
    fun waitingDisconnectFinalizeIsNoOpAfterReconnect() = runBlocking {
        val room = room()
        room.addPlayer("Alice", isHost = true)
        val bob = room.addPlayer("Bob")

        room.handleDisconnect(bob)
        room.handleReconnect(bob)
        room.finalizeWaitingDisconnect(bob) // must not remove a reconnected player

        assertEquals(2, room.getHumanPlayerCount())
        assertTrue(room.getPlayerSession(bob)!!.isConnected)
        room.cleanup()
    }

    @Test
    fun hostKeepsHostWhileDisconnectedThenTransfersAfterGrace() = runBlocking {
        val room = room()
        val alice = room.addPlayer("Alice", isHost = true)
        val bob = room.addPlayer("Bob")

        room.handleDisconnect(alice)
        assertTrue(room.isHost(alice), "host retained during grace window")
        assertEquals(2, room.getHumanPlayerCount())

        room.finalizeWaitingDisconnect(alice)
        assertTrue(room.isHost(bob), "host transferred only after the window elapses")
        assertEquals(1, room.getHumanPlayerCount())
        room.cleanup()
    }

    @Test
    fun guestCanJoinWhileHostBrieflyDisconnected() = runBlocking {
        // Scenario: host backgrounds to share the invite; guest joins during that window.
        val room = room()
        val alice = room.addPlayer("Alice", isHost = true)
        room.handleDisconnect(alice)         // host backgrounds — socket drops

        val bob = room.addPlayer("Bob")      // guest joins via the code

        assertEquals(2, room.getHumanPlayerCount(), "guest joins the still-alive room")
        assertTrue(room.isHost(alice), "disconnected host keeps host during grace")
        assertFalse(room.isHost(bob))

        assertTrue(room.handleReconnect(alice), "host reconnects into the same room")
        assertTrue(room.getPlayerSession(alice)!!.isConnected)
        room.cleanup()
    }

    @Test
    fun orphanedRoomGivesHostToNextJoiner() = runBlocking {
        // Host leaves and the grace window elapses, leaving the room with a stale
        // hostId; a late joiner via the old code must claim host, not get stuck.
        val room = room()
        val alice = room.addPlayer("Alice", isHost = true)
        room.handleDisconnect(alice)
        room.finalizeWaitingDisconnect(alice) // alice removed; room now empty
        assertEquals(0, room.getHumanPlayerCount())

        val carol = room.addPlayer("Carol")
        assertTrue(room.isHost(carol), "joiner claims host when none is valid")
        room.cleanup()
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
