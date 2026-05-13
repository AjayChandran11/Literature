package com.cards.game.literature.bot

import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BotStrategyTest {
    private val strategy = BotStrategy()

    // 6-player state: bot=p1 (t1), teammates=p3/p5 (t1), opponents=p2/p4/p6 (t2)
    private fun makeState(
        botHand: List<Card>,
        p3Hand: List<Card> = listOf(Card(Suit.HEARTS, CardValue.TWO)),
        p5Hand: List<Card> = listOf(Card(Suit.HEARTS, CardValue.THREE)),
        p2Hand: List<Card> = listOf(Card(Suit.CLUBS, CardValue.TWO)),
        p4Hand: List<Card> = listOf(Card(Suit.CLUBS, CardValue.THREE)),
        p6Hand: List<Card> = listOf(Card(Suit.CLUBS, CardValue.FOUR)),
        events: List<GameEvent> = emptyList()
    ) = GameState(
        gameId = "test",
        players = listOf(
            Player("p1", "Bot", "t1", botHand, isBot = true),
            Player("p2", "P2",  "t2", p2Hand),
            Player("p3", "P3",  "t1", p3Hand),
            Player("p4", "P4",  "t2", p4Hand),
            Player("p5", "P5",  "t1", p5Hand),
            Player("p6", "P6",  "t2", p6Hand),
        ),
        teams = listOf(
            Team("t1", "Team 1", listOf("p1", "p3", "p5")),
            Team("t2", "Team 2", listOf("p2", "p4", "p6"))
        ),
        currentPlayerIndex = 0,
        phase = GamePhase.IN_PROGRESS,
        events = events,
        playerCount = 6
    )

    private fun asked(askerId: String, targetId: String, card: Card, success: Boolean) =
        GameEvent.CardAsked(askerId, askerId, targetId, targetId, card, success)

    @Test
    fun claimsWhenTeamHasAllSixCardsConfirmed() {
        val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
        val botCards = spadesLow.take(3)
        val p3Cards = spadesLow.drop(3)
        val events = p3Cards.map { asked("p3", "p2", it, success = true) }

        val state = makeState(botHand = botCards, p3Hand = p3Cards, events = events)
        val action = strategy.decideMove(state, "p1", BotDifficulty.MEDIUM)

        assertIs<BotAction.Claim>(action)
        assertEquals(HalfSuit.SPADES_LOW, action.declaration.halfSuit)
    }

    @Test
    fun claimAssignmentMatchesActualDistribution() {
        val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
        val botCards = spadesLow.take(3)
        val p3Cards = spadesLow.drop(3)
        val events = p3Cards.map { asked("p3", "p2", it, success = true) }

        val state = makeState(botHand = botCards, p3Hand = p3Cards, events = events)
        val action = strategy.decideMove(state, "p1", BotDifficulty.MEDIUM) as BotAction.Claim

        assertEquals(botCards.toSet(), action.declaration.cardAssignments["p1"]?.toSet())
        assertEquals(p3Cards.toSet(), action.declaration.cardAssignments["p3"]?.toSet())
    }

    @Test
    fun claimsWhenSixthCardDeducedViaElimination() {
        val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
        val botCards = spadesLow.take(5)
        val s7 = spadesLow.last()
        // Rule out p1, p2, p4, p5, p6 for s7 → only p3 remains → deduction fires → claim
        val events = listOf(
            asked("p1", "p2", s7, success = false),
            asked("p4", "p6", s7, success = false),
            asked("p5", "p2", s7, success = false)
        )

        val state = makeState(
            botHand = botCards,
            p3Hand = listOf(s7, Card(Suit.HEARTS, CardValue.TWO)),
            events = events
        )
        val action = strategy.decideMove(state, "p1", BotDifficulty.MEDIUM)

        assertIs<BotAction.Claim>(action)
        assertEquals(HalfSuit.SPADES_LOW, action.declaration.halfSuit)
    }

    @Test
    fun asksForCardKnownToBeWithOpponent() {
        val s2 = Card(Suit.SPADES, CardValue.TWO)
        val s7 = Card(Suit.SPADES, CardValue.SEVEN)
        val events = listOf(asked("p2", "p4", s7, success = true))

        val state = makeState(botHand = listOf(s2), p2Hand = listOf(s7), events = events)
        val action = strategy.decideMove(state, "p1", BotDifficulty.MEDIUM)

        assertIs<BotAction.Ask>(action)
        assertEquals("p2", action.targetId)
        assertEquals(s7, action.card)
    }

    @Test
    fun askTargetIsAlwaysAnOpponent() {
        val s2 = Card(Suit.SPADES, CardValue.TWO)
        val state = makeState(botHand = listOf(s2))
        val action = strategy.decideMove(state, "p1", BotDifficulty.MEDIUM)

        assertIs<BotAction.Ask>(action)
        assertTrue(action.targetId in listOf("p2", "p4", "p6"),
            "Expected opponent target, got ${action.targetId}")
    }

    @Test
    fun askCardIsAlwaysFromBotHalfSuit() {
        val s2 = Card(Suit.SPADES, CardValue.TWO) // bot only holds SPADES_LOW
        val state = makeState(botHand = listOf(s2))
        val action = strategy.decideMove(state, "p1", BotDifficulty.MEDIUM)

        assertIs<BotAction.Ask>(action)
        assertEquals(HalfSuit.SPADES_LOW, DeckUtils.getHalfSuit(action.card))
    }

    @Test
    fun askCardIsNotAlreadyInBotHand() {
        val botCards = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW).take(3)
        val state = makeState(botHand = botCards)
        val action = strategy.decideMove(state, "p1", BotDifficulty.MEDIUM)

        assertIs<BotAction.Ask>(action)
        assertTrue(action.card !in botCards, "Bot should not ask for a card it already holds")
    }

    @Test
    fun hardBotClaimsViaSameLogicAsmediumWhenCertain() {
        val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
        val botCards = spadesLow.take(3)
        val p3Cards = spadesLow.drop(3)
        val events = p3Cards.map { asked("p3", "p2", it, success = true) }

        val state = makeState(botHand = botCards, p3Hand = p3Cards, events = events)
        val action = strategy.decideMove(state, "p1", BotDifficulty.HARD)

        assertIs<BotAction.Claim>(action)
        assertEquals(HalfSuit.SPADES_LOW, action.declaration.halfSuit)
    }
}
