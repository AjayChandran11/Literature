package com.cards.game.literature.server

import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.GameState
import com.cards.game.literature.model.Player
import com.cards.game.literature.model.Team
import com.cards.game.literature.protocol.GameVariants
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Game Variants (turn timer) server config plumbing: updateVariants sanitizes and
 * surfaces in RoomState, the derived turn timeout honors Off, and settings can't
 * change once the game has started. (The host + phase user-facing rejection lives
 * in GameWebSocket; here we cover the room-level behavior.)
 */
class GameRoomVariantsTest {

    private fun waitingRoom(): GameRoom {
        val room = GameRoom("TEST01", 4)
        room.addPlayer("Alice", isHost = true)
        room.addPlayer("Bob")
        return room
    }

    @Test
    fun defaultsToClassicSixtySecondTimer() {
        val room = waitingRoom()
        assertEquals(60, room.toRoomState().variants.turnTimerSeconds)
        assertEquals(60_000L, room.turnTimeoutMs())
    }

    @Test
    fun updateVariantsSurfacesInRoomStateAndTimeout() {
        val room = waitingRoom()
        room.updateVariants(GameVariants(turnTimerSeconds = 30))
        assertEquals(30, room.toRoomState().variants.turnTimerSeconds)
        assertEquals(30_000L, room.turnTimeoutMs())
    }

    @Test
    fun offDisablesTheTimer() {
        val room = waitingRoom()
        room.updateVariants(GameVariants(turnTimerSeconds = null))
        assertNull(room.toRoomState().variants.turnTimerSeconds)
        assertNull(room.turnTimeoutMs()) // Off → startTurnTimer arms nothing
    }

    @Test
    fun unsupportedTimerValueIsSanitizedToDefault() {
        val room = waitingRoom()
        room.updateVariants(GameVariants(turnTimerSeconds = 12345))
        assertEquals(60, room.toRoomState().variants.turnTimerSeconds)
    }

    @Test
    fun settingsCannotChangeOnceGameStarted() = runBlocking {
        val room = waitingRoom()
        room.updateVariants(GameVariants(turnTimerSeconds = 30))
        // Flip the room to IN_PROGRESS via the test hook, then try to change again.
        room.installRunningGameForTest(
            GameState(
                gameId = "variants-test",
                players = listOf(
                    Player("player_0", "Alice", "t1", emptyList()),
                    Player("player_1", "Bob", "t2", emptyList())
                ),
                teams = listOf(
                    Team("t1", "Team 1", listOf("player_0")),
                    Team("t2", "Team 2", listOf("player_1"))
                ),
                currentPlayerIndex = 0,
                phase = GamePhase.IN_PROGRESS,
                playerCount = 2
            )
        )
        room.updateVariants(GameVariants(turnTimerSeconds = 90)) // must be ignored mid-game
        assertEquals(30, room.toRoomState().variants.turnTimerSeconds)
        room.cleanup()
    }
}
