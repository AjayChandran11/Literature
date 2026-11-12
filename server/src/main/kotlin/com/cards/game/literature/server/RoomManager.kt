package com.cards.game.literature.server

import com.cards.game.literature.protocol.RoomPhase
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class RoomManager {
    companion object {
        /** Quick Play tables are small so they deal soon and the games stay short. */
        const val QUICK_PLAY_PLAYER_COUNT = 4
        /** How long a fresh Quick Play table waits for company before dealing with bots. The host
         *  can start sooner from the button, so this only bounds the wait, never the game. */
        const val QUICK_START_DELAY_MS = 60_000L
    }

    private val log = LoggerFactory.getLogger("RoomManager")
    private val rooms = ConcurrentHashMap<String, GameRoom>()
    /** Quick Play's find-or-create runs under this: as two steps, four people pressing the button
     *  in the same instant opened three tables instead of filling one. */
    private val quickPlayLock = Any()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        cleanupScope.launch {
            while (isActive) {
                delay(60_000)
                cleanupStaleRooms()
            }
        }
    }

    fun createRoom(hostName: String, playerCount: Int, isPublic: Boolean = false): Pair<GameRoom, String> {
        val roomCode = generateRoomCode()
        val room = GameRoom(roomCode, playerCount, isPublic)
        rooms[roomCode] = room
        val playerId = room.addPlayer(hostName, isHost = true)
        log.info("Room {} created for {} players", roomCode, playerCount)
        return Pair(room, playerId)
    }

    fun getRoom(roomCode: String): GameRoom? = rooms[roomCode.uppercase()]

    /**
     * The public tables a Quick Play may sit down at, fullest first so one table fills and deals
     * rather than two half-empty ones waiting on each other: still waiting to deal, a seat free
     * (or freeable), and someone actually there. "Fullest" counts people present, not held
     * seats, and a table where everyone is away is not offered at all — its countdown has
     * already declined to deal, and whoever sat down there waited as the guest of a host who
     * was gone. Never a running game, whatever seats it has. Ties go to the table about to
     * deal, then to the one that has waited longest; as a plain sort they went to hash order,
     * which also made the ranking test a coin flip.
     */
    fun openPublicTables(): List<GameRoom> =
        rooms.values.filter { it.isPublic && !it.isClosed && it.isJoinable() && it.connectedPlayerCount() > 0 }
            .sortedWith(
                compareByDescending<GameRoom> { it.connectedPlayerCount() }
                    .thenBy { it.autoStartAtMs ?: Long.MAX_VALUE }
                    .thenBy { it.createdSeq }
            )

    fun findOpenPublicTable(): GameRoom? = openPublicTables().firstOrNull()

    /**
     * Quick Play as one step: the fullest open table that will have them, else a fresh one. The
     * table counts down from here if it was not already — an "Open to anyone" room or a rematch
     * lobby has no clock of its own, and a stranger must never be left waiting on a host they
     * do not know. Held seats are not reclaimed by name here (see [GameRoom.trySeat]).
     */
    fun seatQuickPlayer(name: String): Pair<GameRoom, String> = synchronized(quickPlayLock) {
        val seated = openPublicTables().firstNotNullOfOrNull { table ->
            (table.trySeat(name, reclaimByName = false) as? GameRoom.SeatResult.Seated)
                ?.let { table to it.playerId }
        }
        if (seated != null) {
            if (seated.first.autoStartAtMs == null) seated.first.scheduleAutoStart(QUICK_START_DELAY_MS)
            return@synchronized seated
        }
        createRoom(name, QUICK_PLAY_PLAYER_COUNT, isPublic = true)
            .also { (fresh, _) -> fresh.scheduleAutoStart(QUICK_START_DELAY_MS) }
    }

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
