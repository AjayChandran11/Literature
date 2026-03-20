package com.cards.game.literature.server

import com.cards.game.literature.protocol.RoomPhase
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class RoomManager {
    private val log = LoggerFactory.getLogger("RoomManager")
    private val rooms = ConcurrentHashMap<String, GameRoom>()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        cleanupScope.launch {
            while (isActive) {
                delay(60_000)
                cleanupStaleRooms()
            }
        }
    }

    fun createRoom(hostName: String, playerCount: Int): Pair<GameRoom, String> {
        val roomCode = generateRoomCode()
        val room = GameRoom(roomCode, playerCount)
        rooms[roomCode] = room
        val playerId = room.addPlayer(hostName, isHost = true)
        log.info("Room {} created by '{}' for {} players", roomCode, hostName, playerCount)
        return Pair(room, playerId)
    }

    fun getRoom(roomCode: String): GameRoom? = rooms[roomCode.uppercase()]

    fun removeRoom(roomCode: String) {
        rooms.remove(roomCode)?.cleanup()
        log.info("Room {} removed", roomCode)
    }

    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var code: String
        do {
            code = (1..6).map { chars.random() }.joinToString("")
        } while (rooms.containsKey(code))
        return code
    }

    private fun cleanupStaleRooms() {
        val now = System.currentTimeMillis()
        val staleRooms = rooms.filter { (_, room) ->
            (room.phase == RoomPhase.FINISHED && now - room.finishedAt > 5 * 60_000) ||
                (room.phase == RoomPhase.WAITING && now - room.createdAt > 30 * 60_000) ||
                room.allDisconnected()
        }
        if (staleRooms.isNotEmpty()) {
            log.info("Cleaning up {} stale room(s): {}", staleRooms.size, staleRooms.keys)
        }
        staleRooms.forEach { (code, _) -> removeRoom(code) }
    }

    fun shutdown() {
        cleanupScope.cancel()
        rooms.values.forEach { it.cleanup() }
        rooms.clear()
    }
}
