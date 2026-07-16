package com.cards.game.literature.protocol

import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.Team
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Wire-compat contract for Game Variants (turn timer). New fields are defaulted and
 * ride existing message types (only one new client→server subtype), so an updated
 * server and an un-updated client can talk without either crashing.
 */
class GameVariantsWireCompatTest {
    // Mirrors the client: unknown keys from a newer peer are dropped, not fatal.
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun oldRoomStateWithoutVariantsDecodesToClassicDefault() {
        // A RoomState from an older server omits the variants key entirely; the new
        // field must default to the classic 60s timer rather than fail to decode.
        val legacy = """{"roomCode":"ABC123","phase":"WAITING","players":[],""" +
            """"hostPlayerId":"p0","targetPlayerCount":4}"""
        val decoded = json.decodeFromString<RoomState>(legacy)
        assertEquals(60, decoded.variants.turnTimerSeconds)
    }

    @Test
    fun roomStateVariantsRoundTrip() {
        val state = RoomState(
            roomCode = "ABC123",
            phase = RoomPhase.WAITING,
            players = emptyList(),
            hostPlayerId = "p0",
            targetPlayerCount = 4,
            variants = GameVariants(turnTimerSeconds = null) // Off
        )
        val decoded = json.decodeFromString<RoomState>(json.encodeToString(state))
        assertNull(decoded.variants.turnTimerSeconds)
    }

    @Test
    fun updateRoomSettingsRoundTrips() {
        val msg: ClientMessage = ClientMessage.UpdateRoomSettings(GameVariants(30))
        val decoded = json.decodeFromString<ClientMessage>(json.encodeToString(msg))
        assertEquals(msg, decoded)
    }

    @Test
    fun turnTimerSecondsRoundTripsAndDefaultsNull() {
        val base = PlayerGameView(
            myPlayerId = "p1",
            myHand = emptyList(),
            players = listOf(PublicPlayerInfo("p1", "P1", "t1", 0, isBot = false)),
            teams = listOf(Team("t1", "Team 1", listOf("p1"))),
            currentPlayerId = "p1",
            phase = GamePhase.IN_PROGRESS,
            halfSuitStatuses = emptyList(),
            recentEvents = emptyList()
        )
        // Absent on an older/offline payload → null → client shows no countdown.
        assertNull(json.decodeFromString<PlayerGameView>(json.encodeToString(base)).turnTimerSeconds)

        val timed = base.copy(turnTimerSeconds = 30)
        assertEquals(
            30,
            json.decodeFromString<PlayerGameView>(json.encodeToString(timed)).turnTimerSeconds
        )
    }

    @Test
    fun sanitizeCoercesUnsupportedTimerToDefault() {
        assertEquals(60, GameVariants(turnTimerSeconds = 12345).sanitized().turnTimerSeconds)
        assertNull(GameVariants(turnTimerSeconds = null).sanitized().turnTimerSeconds)
        assertEquals(30, GameVariants(turnTimerSeconds = 30).sanitized().turnTimerSeconds)
    }
}
