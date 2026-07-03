package com.cards.game.literature.protocol

import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.PendingPass
import com.cards.game.literature.model.Team
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Guards the wire-compat contract for Option C. The new fields are defaulted and
 * ride existing message types (no new GamePhase/GameEvent/ServerMessage subtypes),
 * so an updated server and an un-updated client can talk without either crashing.
 */
class OptionCWireCompatTest {
    // Mirrors the client: unknown keys from a newer peer are dropped, not fatal.
    private val json = Json { ignoreUnknownKeys = true }

    private val idleView = PlayerGameView(
        myPlayerId = "p1",
        myHand = emptyList(),
        players = listOf(PublicPlayerInfo("p1", "P1", "t1", 0, isBot = false)),
        teams = listOf(Team("t1", "Team 1", listOf("p1"))),
        currentPlayerId = "p1",
        phase = GamePhase.IN_PROGRESS,
        halfSuitStatuses = emptyList(),
        recentEvents = emptyList()
    )

    @Test
    fun idleViewRoundTripsWithNoPendingPass() {
        // A payload from an older server carries no pendingPass key at all; the
        // new field must default to null rather than fail to decode.
        val decoded = json.decodeFromString<PlayerGameView>(json.encodeToString(idleView))
        assertNull(decoded.pendingPass)
        assertNull(decoded.pendingPassDeadlineMs)
    }

    @Test
    fun pendingPassRoundTripsIntact() {
        val pending = idleView.copy(
            pendingPass = PendingPass("p1", listOf("p3", "p5")),
            pendingPassDeadlineMs = 1_700_000_000_000L
        )
        val decoded = json.decodeFromString<PlayerGameView>(json.encodeToString(pending))

        assertEquals("p1", decoded.pendingPass?.claimerId)
        assertEquals(listOf("p3", "p5"), decoded.pendingPass?.eligibleTeammateIds)
        assertEquals("p3", decoded.pendingPass?.defaultTarget)
        assertEquals(1_700_000_000_000L, decoded.pendingPassDeadlineMs)
    }

    @Test
    fun selectPassTargetRoundTrips() {
        val msg: ClientMessage = ClientMessage.SelectPassTarget("p5")
        val decoded = json.decodeFromString<ClientMessage>(json.encodeToString(msg))
        assertEquals(msg, decoded)
    }
}
