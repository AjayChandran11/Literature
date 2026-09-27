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
        val room = rooms.remove(roomCode) ?: return
        // Announce it before it stops existing. A client whose socket is still open otherwise
        // played on into a room the registry had already dropped.
        cleanupScope.launch { room.closeAndNotify() }
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

    private fun cleanupStaleRooms() = sweepStaleRooms(System.currentTimeMillis())

    /**
     * Removes rooms nobody will come back to. The two age rules (a finished room five minutes
     * after the finish, a waiting room half an hour after creation) only apply once everyone
     * has disconnected: they used to fire under connected players — a group chatting on the
     * result screen for five minutes then tapping Rematch played on in a room that no longer
     * existed, so every invite and every reconnect got "Room not found".
     */
    internal fun sweepStaleRooms(now: Long) {
        val staleRooms = rooms.filter { (_, room) ->
            val nobodyHere = room.allDisconnected()
            (nobodyHere && room.phase == RoomPhase.FINISHED && now - room.finishedAt > 5 * 60_000) ||
                (nobodyHere && room.phase == RoomPhase.WAITING && now - room.createdAt > 30 * 60_000) ||
                // isAbandoned (not allDisconnected) — respects pending reconnect
                // deadlines so a group WiFi blip doesn't delete an active game
                room.isAbandoned(now)
        }
        if (staleRooms.isNotEmpty()) {
            log.info("Cleaning up {} stale room(s): {}", staleRooms.size, staleRooms.keys)
        }
        staleRooms.forEach { (code, _) -> removeRoom(code) }
    }

    fun shutdown() {
        val open = rooms.values.toList()
        rooms.clear()
        // Best effort only, and deliberately not wired to a JVM shutdown hook: Netty's event
        // loops are daemon threads, so they are already being torn down while hooks run and the
        // frames are simply never written (measured — the sends are rejected with "event
        // executor terminated"). Delivering a goodbye on SIGTERM would need a signal handler
        // running before JVM shutdown begins. What this does guarantee is that the rooms are
        // marked closed and their sockets released; the clients then reconnect and are told
        // the room is gone, which is the same outcome with one extra round trip.
        // Bounded: shutdown must not hang on a socket that will not drain.
        runBlocking {
            withTimeoutOrNull(2_000) {
                open.map { room -> async { room.closeAndNotify() } }.awaitAll()
            }
        }
        open.forEach { it.cleanup() }
        cleanupScope.cancel()
    }
}
