package com.cards.game.literature.server

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomManagerTest {

    @Test
    fun createRoomReturnsWellFormedCodeAndHost() {
        val manager = RoomManager()
        try {
            val (room, playerId) = manager.createRoom("Alice", 4)

            assertEquals(6, room.roomCode.length)
            // Charset excludes easily-confused characters (I, O, 0, 1)
            assertTrue(room.roomCode.all { it in "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" })
            assertEquals("player_0", playerId)
            assertTrue(room.isHost(playerId))
            assertEquals(4, room.targetPlayerCount)
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun getRoomIsCaseInsensitive() {
        val manager = RoomManager()
        try {
            val (room, _) = manager.createRoom("Alice", 4)
            assertEquals(room, manager.getRoom(room.roomCode.lowercase()))
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun aRemovedRoomIsClosedSoNothingKeepsPlayingIntoIt() = runBlocking {
        val manager = RoomManager()
        try {
            val (room, _) = manager.createRoom("Alice", 4)
            manager.removeRoom(room.roomCode)

            // removeRoom announces the closure off the caller's thread, so wait for the flag
            // rather than assume it lands synchronously.
            withTimeout(2_000) { while (!room.isClosed) delay(10) }
            assertNull(manager.getRoom(room.roomCode), "and it is gone from the registry")
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun shutdownClosesEveryRoomItWasHolding() {
        val manager = RoomManager()
        val (first, _) = manager.createRoom("Alice", 4)
        val (second, _) = manager.createRoom("Bob", 6)

        manager.shutdown()

        assertTrue(first.isClosed && second.isClosed, "every room is marked closed and released")
    }

    @Test
    fun removeRoomMakesItUnreachable() {
        val manager = RoomManager()
        try {
            val (room, _) = manager.createRoom("Alice", 4)
            manager.removeRoom(room.roomCode)
            assertNull(manager.getRoom(room.roomCode))
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun ageBasedSweepSparesRoomsWithConnectedPlayers() {
        val manager = RoomManager()
        try {
            val (room, hostId) = manager.createRoom("Alice", 4)
            val farFuture = System.currentTimeMillis() + 31 * 60_000 // well past the 30-min waiting-room age

            manager.sweepStaleRooms(farFuture)
            assertEquals(room, manager.getRoom(room.roomCode), "an old room with a connected player must stay")

            room.getPlayerSession(hostId)!!.isConnected = false
            manager.sweepStaleRooms(farFuture)
            assertNull(manager.getRoom(room.roomCode), "once everyone is gone the age rule applies")
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun roomCodesAreUnique() {
        val manager = RoomManager()
        try {
            val codes = (1..50).map { manager.createRoom("P$it", 4).first.roomCode }
            assertEquals(codes.size, codes.toSet().size)
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun quickPlayFindsTheFullestWaitingPublicTableAndNeverARunningOne() = runBlocking {
        val manager = RoomManager()
        try {
            val (one, _) = manager.createRoom("A", 4, isPublic = true)
            val (two, _) = manager.createRoom("B", 4, isPublic = true)
            two.addPlayer("B2")
            manager.createRoom("C", 4, isPublic = false)

            assertEquals(two, manager.findOpenPublicTable(), "two people waiting beats one; private tables never appear")

            two.getPlayerSession("player_1")!!.isConnected = false
            assertEquals(one, manager.findOpenPublicTable(), "people present count, not seats held")
            two.getPlayerSession("player_0")!!.isConnected = false
            one.getPlayerSession("player_0")!!.isConnected = false
            assertNull(manager.findOpenPublicTable(), "a table where everyone is away is not offered to anyone")
            listOf(one, two).forEach { room -> room.toRoomState().players.forEach { room.getPlayerSession(it.id)!!.isConnected = true } }

            assertTrue(two.startGame(fillWithBots = true))
            assertEquals(one, manager.findOpenPublicTable(), "a running table is never offered")

            assertTrue(one.startGame(fillWithBots = true))
            assertNull(manager.findOpenPublicTable(), "no waiting public table means Quick Play opens a new one")
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun quickPlayStartsTheClockAtATableThatHadNone() {
        val manager = RoomManager()
        try {
            // "Open to anyone": the host decides when to deal, so the room has no countdown of its own.
            val (open, host) = manager.createRoom("Host", 4, isPublic = true)
            assertNull(open.autoStartAtMs)

            val (table, seat) = manager.seatQuickPlayer("Stranger")
            assertEquals(open, table, "the stranger sits down at the open room")
            assertFalse(table.isHost(seat))
            assertTrue(table.isHost(host))
            assertNotNull(table.autoStartAtMs, "and it deals itself from here: a stranger never waits on a host they do not know")
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun quickPlayNeverHandsAStrangerAHeldSeat() {
        val manager = RoomManager()
        try {
            val (table, sam) = manager.seatQuickPlayer("Sam")
            manager.seatQuickPlayer("Tara")
            val samToken = table.getPlayerSession(sam)!!.reconnectToken
            table.getPlayerSession(sam)!!.isConnected = false // Sam's app is in the background; the seat is held

            val (again, stranger) = manager.seatQuickPlayer("sam")
            assertEquals(table, again)
            assertNotEquals(sam, stranger, "the same display name earns a new seat on a public table, not Sam's")
            assertNotEquals(samToken, table.getPlayerSession(stranger)!!.reconnectToken, "and its own token")
            assertEquals("Sam", table.getPlayerSession(sam)!!.playerName, "Sam's seat is still Sam's")
            assertEquals(3, table.getHumanPlayerCount())

            // By room code it is still a return: there the code is the secret.
            val back = table.trySeat("sam")
            assertIs<GameRoom.SeatResult.Seated>(back)
            assertEquals(sam, back.playerId)
            assertTrue(back.reclaimed)
        } finally {
            manager.shutdown()
        }
    }

    @Test
    fun simultaneousQuickPlaysFillOneTable() {
        val manager = RoomManager()
        val pool = Executors.newFixedThreadPool(4)
        try {
            val go = CountDownLatch(1)
            val presses = (1..4).map { i -> pool.submit<Pair<GameRoom, String>> { go.await(); manager.seatQuickPlayer("P$i") } }
            go.countDown()
            val tables = presses.map { it.get(5, TimeUnit.SECONDS).first }.toSet()

            assertEquals(1, tables.size, "four presses in the same instant share one table rather than opening three")
            assertEquals(4, tables.single().getHumanPlayerCount())
            assertNotEquals(tables.single(), manager.seatQuickPlayer("P5").first, "the fifth opens the next table")
        } finally {
            pool.shutdownNow()
            manager.shutdown()
        }
    }
}
