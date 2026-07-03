package com.cards.game.literature.server

import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.ClaimDeclaration
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.GameState
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.Player
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.Team
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Option C server orchestration. Uses a hand-built 6-player game (t1 has 3
 * members) installed via the test hook, so a correct SPADES_LOW claim empties
 * player_0 while player_2 and player_4 stay active — the only shape that offers
 * a real choice.
 */
class GameRoomOptionCTest {

    private val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
    private val heartsTwo = Card(Suit.HEARTS, CardValue.TWO)
    private val heartsThree = Card(Suit.HEARTS, CardValue.THREE)
    private val clubTwo = Card(Suit.CLUBS, CardValue.TWO)
    private val clubThree = Card(Suit.CLUBS, CardValue.THREE)
    private val clubFour = Card(Suit.CLUBS, CardValue.FOUR)

    private val claim = ClaimDeclaration(
        "player_0", HalfSuit.SPADES_LOW,
        mapOf(
            "player_0" to spadesLow.take(2),
            "player_2" to spadesLow.subList(2, 4),
            "player_4" to spadesLow.subList(4, 6)
        )
    )

    /** Room with 6 added players and a running game where player_0 is on turn. */
    private fun sixPlayerRoom(claimerProtocol: Int = 3): GameRoom {
        val room = GameRoom("TEST01", 6)
        repeat(6) { room.addPlayer("P$it", isHost = it == 0) }
        room.getPlayerSession("player_0")!!.protocolVersion = claimerProtocol

        val players = listOf(
            Player("player_0", "P0", "t1", spadesLow.take(2)),
            Player("player_1", "P1", "t2", listOf(clubTwo)),
            Player("player_2", "P2", "t1", spadesLow.subList(2, 4) + heartsTwo),
            Player("player_3", "P3", "t2", listOf(clubThree)),
            Player("player_4", "P4", "t1", spadesLow.subList(4, 6) + heartsThree),
            Player("player_5", "P5", "t2", listOf(clubFour))
        )
        val teams = listOf(
            Team("t1", "Team 1", listOf("player_0", "player_2", "player_4")),
            Team("t2", "Team 2", listOf("player_1", "player_3", "player_5"))
        )
        room.installRunningGameForTest(
            GameState(
                gameId = "optionc-server-test",
                players = players,
                teams = teams,
                currentPlayerIndex = 0,
                phase = GamePhase.IN_PROGRESS,
                playerCount = 6
            )
        )
        return room
    }

    @Test
    fun v3ClaimerEmptyingWithTwoTeammatesSuspendsForSelection() = runBlocking {
        val room = sixPlayerRoom()
        room.processClaim("player_0", claim)

        val pending = room.pendingPassForTest
        assertNotNull(pending)
        assertEquals("player_0", pending.claimerId)
        assertEquals(listOf("player_2", "player_4"), pending.eligibleTeammateIds)
        assertEquals("player_0", room.currentPlayerId) // turn not handed out yet
        room.cleanup()
    }

    @Test
    fun claimerSelectionMovesTurnAndClearsPending() = runBlocking {
        val room = sixPlayerRoom()
        room.processClaim("player_0", claim)

        room.selectPassTarget("player_0", "player_4") // the non-default teammate
        assertEquals("player_4", room.currentPlayerId)
        assertNull(room.pendingPassForTest)
        room.cleanup()
    }

    @Test
    fun selectionFromNonClaimerIsIgnored() = runBlocking {
        val room = sixPlayerRoom()
        room.processClaim("player_0", claim)

        room.selectPassTarget("player_1", "player_2") // an opponent tries to force it
        assertNotNull(room.pendingPassForTest)          // still suspended
        assertEquals("player_0", room.currentPlayerId)
        room.cleanup()
    }

    @Test
    fun selectionTimeoutAutoPicksDefault() = runBlocking {
        val room = sixPlayerRoom()
        room.passSelectionTimeoutMs = 50
        room.processClaim("player_0", claim)
        assertNotNull(room.pendingPassForTest)

        delay(400) // let the timer fire
        assertNull(room.pendingPassForTest)
        assertEquals("player_2", room.currentPlayerId) // default = first eligible teammate
        room.cleanup()
    }

    @Test
    fun legacyClaimerNeverSuspends() = runBlocking {
        val room = sixPlayerRoom(claimerProtocol = 2)
        room.processClaim("player_0", claim)

        assertNull(room.pendingPassForTest)
        assertEquals("player_2", room.currentPlayerId) // deterministic auto-pass, as before
        room.cleanup()
    }

    @Test
    fun claimerDisconnectResolvesPendingSelection() = runBlocking {
        val room = sixPlayerRoom()
        room.processClaim("player_0", claim)
        assertNotNull(room.pendingPassForTest)

        room.handleDisconnect("player_0")
        assertNull(room.pendingPassForTest)
        assertEquals("player_2", room.currentPlayerId) // default pick unblocks the table
        room.cleanup()
    }
}
