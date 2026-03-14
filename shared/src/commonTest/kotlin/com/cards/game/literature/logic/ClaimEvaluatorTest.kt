package com.cards.game.literature.logic

import com.cards.game.literature.model.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClaimEvaluatorTest {

    @Test
    fun correctClaimAwardsToClaimingTeam() {
        val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
        val p1Hand = spadesLow.take(3)
        val p3Hand = spadesLow.drop(3)

        val players = listOf(
            Player("p1", "P1", "t1", p1Hand),
            Player("p2", "P2", "t2", listOf(Card(Suit.HEARTS, CardValue.ACE))),
            Player("p3", "P3", "t1", p3Hand),
            Player("p4", "P4", "t2", listOf(Card(Suit.HEARTS, CardValue.TWO)))
        )
        val teams = listOf(
            Team("t1", "Team 1", listOf("p1", "p3")),
            Team("t2", "Team 2", listOf("p2", "p4"))
        )
        val state = GameState("test", players, teams, 0, GamePhase.IN_PROGRESS, playerCount = 4)

        val declaration = ClaimDeclaration(
            claimerId = "p1",
            halfSuit = HalfSuit.SPADES_LOW,
            cardAssignments = mapOf("p1" to p1Hand, "p3" to p3Hand)
        )

        val result = ClaimEvaluator.evaluate(state, declaration)
        assertTrue(result.correct)
        assertTrue(result.awardedToTeamId == "t1")
    }

    @Test
    fun incorrectClaimAwardsToOpponent() {
        val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
        val p1Hand = spadesLow.take(3)
        val p3Hand = spadesLow.drop(3)

        val players = listOf(
            Player("p1", "P1", "t1", p1Hand),
            Player("p2", "P2", "t2", listOf(Card(Suit.HEARTS, CardValue.ACE))),
            Player("p3", "P3", "t1", p3Hand),
            Player("p4", "P4", "t2", listOf(Card(Suit.HEARTS, CardValue.TWO)))
        )
        val teams = listOf(
            Team("t1", "Team 1", listOf("p1", "p3")),
            Team("t2", "Team 2", listOf("p2", "p4"))
        )
        val state = GameState("test", players, teams, 0, GamePhase.IN_PROGRESS, playerCount = 4)

        // Wrong assignment: swap p1 and p3 cards
        val declaration = ClaimDeclaration(
            claimerId = "p1",
            halfSuit = HalfSuit.SPADES_LOW,
            cardAssignments = mapOf("p1" to p3Hand, "p3" to p1Hand)
        )

        val result = ClaimEvaluator.evaluate(state, declaration)
        assertFalse(result.correct)
        assertTrue(result.awardedToTeamId == "t2")
    }
}
