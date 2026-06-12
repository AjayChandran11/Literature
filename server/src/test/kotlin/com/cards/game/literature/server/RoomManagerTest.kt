package com.cards.game.literature.server

import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun roomCodesAreUnique() {
        val manager = RoomManager()
        try {
            val codes = (1..50).map { manager.createRoom("P$it", 4).first.roomCode }
            assertEquals(codes.size, codes.toSet().size)
        } finally {
            manager.shutdown()
        }
    }
}
