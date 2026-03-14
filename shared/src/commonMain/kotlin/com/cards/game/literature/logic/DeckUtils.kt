package com.cards.game.literature.logic

import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.Suit

object DeckUtils {

    private val lowValues = listOf(
        CardValue.TWO, CardValue.THREE, CardValue.FOUR,
        CardValue.FIVE, CardValue.SIX, CardValue.SEVEN
    )

    private val highValues = listOf(
        CardValue.NINE, CardValue.TEN, CardValue.JACK,
        CardValue.QUEEN, CardValue.KING, CardValue.ACE
    )

    fun getHalfSuit(card: Card): HalfSuit {
        val isLow = card.value in lowValues
        return when (card.suit) {
            Suit.SPADES -> if (isLow) HalfSuit.SPADES_LOW else HalfSuit.SPADES_HIGH
            Suit.HEARTS -> if (isLow) HalfSuit.HEARTS_LOW else HalfSuit.HEARTS_HIGH
            Suit.DIAMONDS -> if (isLow) HalfSuit.DIAMONDS_LOW else HalfSuit.DIAMONDS_HIGH
            Suit.CLUBS -> if (isLow) HalfSuit.CLUBS_LOW else HalfSuit.CLUBS_HIGH
        }
    }

    fun getAllCardsForHalfSuit(halfSuit: HalfSuit): List<Card> {
        val suit = when (halfSuit) {
            HalfSuit.SPADES_LOW, HalfSuit.SPADES_HIGH -> Suit.SPADES
            HalfSuit.HEARTS_LOW, HalfSuit.HEARTS_HIGH -> Suit.HEARTS
            HalfSuit.DIAMONDS_LOW, HalfSuit.DIAMONDS_HIGH -> Suit.DIAMONDS
            HalfSuit.CLUBS_LOW, HalfSuit.CLUBS_HIGH -> Suit.CLUBS
        }
        val values = when (halfSuit) {
            HalfSuit.SPADES_LOW, HalfSuit.HEARTS_LOW,
            HalfSuit.DIAMONDS_LOW, HalfSuit.CLUBS_LOW -> lowValues
            else -> highValues
        }
        return values.map { Card(suit, it) }
    }

    fun createFullDeck(): List<Card> {
        return HalfSuit.entries.flatMap { getAllCardsForHalfSuit(it) }
    }
}
