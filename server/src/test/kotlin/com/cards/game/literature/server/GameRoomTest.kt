package com.cards.game.literature.server

import com.cards.game.literature.protocol.RoomPhase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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
}
