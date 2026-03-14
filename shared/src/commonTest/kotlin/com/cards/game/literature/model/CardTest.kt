package com.cards.game.literature.model

import com.cards.game.literature.logic.DeckUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardTest {
    @Test
    fun fullDeckHas48Cards() {
        val deck = DeckUtils.createFullDeck()
        assertEquals(48, deck.size)
    }

    @Test
    fun noEightsInDeck() {
        val deck = DeckUtils.createFullDeck()
        assertTrue(deck.none { it.value.rank == 8 })
    }

    @Test
    fun eachHalfSuitHas6Cards() {
        HalfSuit.entries.forEach { hs ->
            assertEquals(6, DeckUtils.getAllCardsForHalfSuit(hs).size)
        }
    }

    @Test
    fun lowSpadesContainsCorrectCards() {
        val cards = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
        val values = cards.map { it.value }
        assertTrue(CardValue.TWO in values)
        assertTrue(CardValue.SEVEN in values)
        assertTrue(CardValue.ACE !in values)
        assertTrue(CardValue.NINE !in values)
    }

    @Test
    fun highSpadesContainsAce() {
        val cards = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_HIGH)
        val values = cards.map { it.value }
        assertTrue(CardValue.ACE in values)
        assertTrue(CardValue.KING in values)
        assertTrue(CardValue.SEVEN !in values)
    }

    @Test
    fun getHalfSuitMapsCorrectly() {
        assertEquals(HalfSuit.SPADES_HIGH, DeckUtils.getHalfSuit(Card(Suit.SPADES, CardValue.ACE)))
        assertEquals(HalfSuit.SPADES_HIGH, DeckUtils.getHalfSuit(Card(Suit.SPADES, CardValue.NINE)))
        assertEquals(HalfSuit.HEARTS_LOW, DeckUtils.getHalfSuit(Card(Suit.HEARTS, CardValue.THREE)))
        assertEquals(HalfSuit.CLUBS_LOW, DeckUtils.getHalfSuit(Card(Suit.CLUBS, CardValue.SEVEN)))
    }
}
