package com.cards.game.literature.logic

import com.cards.game.literature.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for GameEngine.processClaim and early-game-end handling.
 * States are built by hand so every card location is deterministic.
 *
 * Layout used throughout: 4 players, t1 = (p1, p3), t2 = (p2, p4),
 * seating order p1, p2, p3, p4. p1 is the current player.
 */
class GameEngineClaimTest {
    private val engine = GameEngine()

    // 2,3,4,5,6,7 of spades — in lowValues order
    private val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
    private val first3 = spadesLow.take(3)
    private val last3 = spadesLow.drop(3)

    private val heartsAce = Card(Suit.HEARTS, CardValue.ACE)
    private val heartsTwo = Card(Suit.HEARTS, CardValue.TWO)
    private val heartsThree = Card(Suit.HEARTS, CardValue.THREE)

    private fun buildState(
        p1Hand: List<Card>,
        p2Hand: List<Card>,
        p3Hand: List<Card>,
        p4Hand: List<Card>,
        halfSuitStatuses: List<HalfSuitStatus> = HalfSuit.entries.map { HalfSuitStatus(it) },
        team1Score: Int = 0,
        team2Score: Int = 0
    ): GameState {
        val players = listOf(
            Player("p1", "Player 1", "t1", p1Hand),
            Player("p2", "Player 2", "t2", p2Hand),
            Player("p3", "Player 3", "t1", p3Hand),
            Player("p4", "Player 4", "t2", p4Hand)
        )
        val teams = listOf(
            Team("t1", "Team 1", listOf("p1", "p3"), score = team1Score),
            Team("t2", "Team 2", listOf("p2", "p4"), score = team2Score)
        )
        return GameState(
            gameId = "claim-test",
            players = players,
            teams = teams,
            currentPlayerIndex = 0,
            phase = GamePhase.IN_PROGRESS,
            halfSuitStatuses = halfSuitStatuses,
            playerCount = 4
        )
    }

    private fun claim(assignments: Map<String, List<Card>>) =
        ClaimDeclaration("p1", HalfSuit.SPADES_LOW, assignments)

    @Test
    fun correctClaimAwardsPointAndRemovesCards() {
        val state = buildState(
            p1Hand = first3 + heartsAce, // extra card keeps claimer active
            p2Hand = listOf(heartsTwo),
            p3Hand = last3,
            p4Hand = listOf(heartsThree)
        )
        val result = engine.processClaim(state, claim(mapOf("p1" to first3, "p3" to last3)))
        val newState = result.newState

        assertEquals(1, newState.getTeam("t1")!!.score)
        assertEquals(0, newState.getTeam("t2")!!.score)

        val status = newState.halfSuitStatuses.first { it.halfSuit == HalfSuit.SPADES_LOW }
        assertEquals("t1", status.claimedByTeamId)
        assertEquals(true, status.claimCorrect)

        // Claimed cards removed from every hand
        assertEquals(listOf(heartsAce), newState.getPlayer("p1")!!.hand)
        assertTrue(newState.getPlayer("p3")!!.hand.isEmpty())

        // Claimer still active — keeps the turn
        assertEquals("p1", newState.currentPlayer.id)

        val claimEvent = result.events.filterIsInstance<GameEvent.DeckClaimed>().single()
        assertTrue(claimEvent.correct)
        assertEquals("t1", claimEvent.teamId)
    }

    @Test
    fun wrongAssignmentBetweenTeammatesAwardsPointToOpponents() {
        val state = buildState(
            p1Hand = first3 + heartsAce,
            p2Hand = listOf(heartsTwo),
            p3Hand = last3,
            p4Hand = listOf(heartsThree)
        )
        // Team holds all 6 cards, but two are assigned to the wrong teammate
        val swapped = mapOf(
            "p1" to listOf(first3[0], first3[1], last3[0]),
            "p3" to listOf(first3[2], last3[1], last3[2])
        )
        val result = engine.processClaim(state, claim(swapped))
        val newState = result.newState

        assertEquals(0, newState.getTeam("t1")!!.score)
        assertEquals(1, newState.getTeam("t2")!!.score)

        val status = newState.halfSuitStatuses.first { it.halfSuit == HalfSuit.SPADES_LOW }
        assertEquals("t2", status.claimedByTeamId)
        assertEquals(false, status.claimCorrect)

        // Cards still removed from all hands on a wrong claim
        assertEquals(listOf(heartsAce), newState.getPlayer("p1")!!.hand)
        assertTrue(newState.getPlayer("p3")!!.hand.isEmpty())

        // Turn passes to the first active opponent
        assertEquals("p2", newState.currentPlayer.id)
    }

    @Test
    fun claimIsWrongWhenOpponentHoldsACard() {
        val state = buildState(
            p1Hand = first3 + heartsAce,
            p2Hand = listOf(last3[2], heartsTwo), // opponent secretly holds 7♠
            p3Hand = last3.take(2),
            p4Hand = listOf(heartsThree)
        )
        val result = engine.processClaim(
            state, claim(mapOf("p1" to first3, "p3" to last3))
        )
        val newState = result.newState

        assertEquals(1, newState.getTeam("t2")!!.score)
        assertFalse(newState.halfSuitStatuses.first { it.halfSuit == HalfSuit.SPADES_LOW }.claimCorrect!!)
        // Opponent's claimed card removed too
        assertEquals(listOf(heartsTwo), newState.getPlayer("p2")!!.hand)
    }

    @Test
    fun claimerOutOfCardsPassesTurnToFirstActiveTeammate() {
        val state = buildState(
            p1Hand = first3, // claim consumes claimer's whole hand
            p2Hand = listOf(heartsTwo),
            p3Hand = last3 + heartsAce, // teammate stays active
            p4Hand = listOf(heartsThree)
        )
        val result = engine.processClaim(state, claim(mapOf("p1" to first3, "p3" to last3)))
        val newState = result.newState

        assertEquals(1, newState.getTeam("t1")!!.score)
        // Deterministic pass to first active teammate, not random
        assertEquals("p3", newState.currentPlayer.id)
        assertEquals(GamePhase.IN_PROGRESS, newState.phase)
    }

    @Test
    fun claimCompletingAllHalfSuitsFinishesGame() {
        // 7 half-suits already claimed: 4 by t1, 3 by t2 (SPADES_LOW is enum
        // index 0, so odd indices = 4 suits go to t1 → t1 wins outright after
        // claiming the last one; an even split would end 4-4, a draw)
        val preClaimed = HalfSuit.entries.mapIndexed { i, hs ->
            if (hs == HalfSuit.SPADES_LOW) HalfSuitStatus(hs)
            else HalfSuitStatus(hs, claimedByTeamId = if (i % 2 == 1) "t1" else "t2", claimCorrect = true)
        }
        val t1Pre = preClaimed.count { it.claimedByTeamId == "t1" }
        val t2Pre = preClaimed.count { it.claimedByTeamId == "t2" }

        val state = buildState(
            p1Hand = first3 + heartsAce,
            p2Hand = listOf(heartsTwo),
            p3Hand = last3,
            p4Hand = listOf(heartsThree),
            halfSuitStatuses = preClaimed,
            team1Score = t1Pre,
            team2Score = t2Pre
        )
        val result = engine.processClaim(state, claim(mapOf("p1" to first3, "p3" to last3)))
        val newState = result.newState

        assertEquals(GamePhase.FINISHED, newState.phase)
        assertEquals(t1Pre + 1, newState.getTeam("t1")!!.score)
        assertEquals(t2Pre, newState.getTeam("t2")!!.score)
        assertTrue(newState.halfSuitStatuses.all { it.claimedByTeamId != null })

        val ended = newState.events.filterIsInstance<GameEvent.GameEnded>().last()
        assertEquals("t1", ended.winnerTeamId)
    }

    @Test
    fun teamEmptiedByOwnClaimGivesUnclaimedSuitsToOpponents() {
        // t1's entire holding is the claimed half-suit — winning the claim
        // empties the team, so the other 7 suits go to t2 as bonus
        val state = buildState(
            p1Hand = first3,
            p2Hand = listOf(heartsTwo),
            p3Hand = last3,
            p4Hand = listOf(heartsThree)
        )
        val result = engine.processClaim(state, claim(mapOf("p1" to first3, "p3" to last3)))
        val newState = result.newState

        assertEquals(GamePhase.FINISHED, newState.phase)
        assertEquals(1, newState.getTeam("t1")!!.score)
        assertEquals(7, newState.getTeam("t2")!!.score)
        assertTrue(newState.halfSuitStatuses.all { it.claimedByTeamId != null })

        val ended = newState.events.filterIsInstance<GameEvent.GameEnded>().last()
        assertEquals("t2", ended.winnerTeamId)
    }

    @Test
    fun askEmptyingOpposingTeamEndsGameWithBonusPoints() {
        // p2 holds t2's last card; p1 takes it with a successful ask
        val spadesFour = spadesLow[2]
        val state = buildState(
            p1Hand = listOf(spadesLow[0], spadesLow[1]),
            p2Hand = listOf(spadesFour),
            p3Hand = listOf(heartsTwo),
            p4Hand = emptyList()
        )
        val result = engine.processAsk(state, "p1", "p2", spadesFour)
        val newState = result.newState

        assertEquals(GamePhase.FINISHED, newState.phase)
        // All 8 unclaimed half-suits awarded to t1
        assertEquals(8, newState.getTeam("t1")!!.score)
        assertEquals(0, newState.getTeam("t2")!!.score)
        assertTrue(newState.halfSuitStatuses.all { it.claimedByTeamId == "t1" })

        val ended = newState.events.filterIsInstance<GameEvent.GameEnded>().last()
        assertEquals("t1", ended.winnerTeamId)
    }
}
