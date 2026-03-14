package com.cards.game.literature.logic

import com.cards.game.literature.model.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MoveValidatorTest {

    private fun createTestState(): GameState {
        val p1Hand = listOf(Card(Suit.SPADES, CardValue.ACE), Card(Suit.SPADES, CardValue.TWO))
        val p2Hand = listOf(Card(Suit.SPADES, CardValue.THREE), Card(Suit.HEARTS, CardValue.ACE))
        val p3Hand = listOf(Card(Suit.HEARTS, CardValue.TWO), Card(Suit.HEARTS, CardValue.THREE))
        val p4Hand = listOf(Card(Suit.DIAMONDS, CardValue.ACE), Card(Suit.DIAMONDS, CardValue.TWO))

        val players = listOf(
            Player("p1", "Player 1", "t1", p1Hand),
            Player("p2", "Player 2", "t2", p2Hand),
            Player("p3", "Player 3", "t1", p3Hand),
            Player("p4", "Player 4", "t2", p4Hand)
        )
        val teams = listOf(
            Team("t1", "Team 1", listOf("p1", "p3")),
            Team("t2", "Team 2", listOf("p2", "p4"))
        )
        return GameState("test", players, teams, currentPlayerIndex = 0, phase = GamePhase.IN_PROGRESS, playerCount = 4)
    }

    @Test
    fun validAsk() {
        val state = createTestState()
        val result = MoveValidator.validateAsk(state, "p1", "p2", Card(Suit.SPADES, CardValue.THREE))
        assertTrue(result.isValid)
    }

    @Test
    fun cannotAskTeammate() {
        val state = createTestState()
        val result = MoveValidator.validateAsk(state, "p1", "p3", Card(Suit.SPADES, CardValue.THREE))
        assertFalse(result.isValid)
    }

    @Test
    fun cannotAskForOwnCard() {
        val state = createTestState()
        val result = MoveValidator.validateAsk(state, "p1", "p2", Card(Suit.SPADES, CardValue.ACE))
        assertFalse(result.isValid)
    }

    @Test
    fun mustHoldCardFromSameHalfSuit() {
        val state = createTestState()
        // p1 has no hearts, so can't ask for hearts
        val result = MoveValidator.validateAsk(state, "p1", "p2", Card(Suit.HEARTS, CardValue.ACE))
        assertFalse(result.isValid)
    }

    @Test
    fun cannotAskWhenNotYourTurn() {
        val state = createTestState()
        val result = MoveValidator.validateAsk(state, "p2", "p1", Card(Suit.SPADES, CardValue.ACE))
        assertFalse(result.isValid)
    }
}
