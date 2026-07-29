package com.cards.game.literature.model

import kotlinx.serialization.Serializable

@Serializable
enum class Suit {
    SPADES, HEARTS, DIAMONDS, CLUBS
}

@Serializable
enum class CardValue(val rank: Int, val displayName: String) {
    ACE(1, "A"),
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    // No 8s in Literature
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K");
}

@Serializable
enum class HalfSuit(val displayName: String) {
    SPADES_LOW("Low Spades"),
    SPADES_HIGH("High Spades"),
    HEARTS_LOW("Low Hearts"),
    HEARTS_HIGH("High Hearts"),
    DIAMONDS_LOW("Low Diamonds"),
    DIAMONDS_HIGH("High Diamonds"),
    CLUBS_LOW("Low Clubs"),
    CLUBS_HIGH("High Clubs");
}

@Serializable
data class Card(
    val suit: Suit,
    val value: CardValue
) {
    val displayName: String
        get() = "${value.displayName}${suit.symbol}"

    override fun toString(): String = displayName
}

val Suit.symbol: String
    get() = when (this) {
        Suit.SPADES -> "\u2660"
        Suit.HEARTS -> "\u2665"
        Suit.DIAMONDS -> "\u2666"
        Suit.CLUBS -> "\u2663"
    }

val Suit.isRed: Boolean
    get() = this == Suit.HEARTS || this == Suit.DIAMONDS

/** The playing-card suit a half-suit belongs to (e.g. SPADES_LOW → SPADES). */
val HalfSuit.suit: Suit
    get() = when (this) {
        HalfSuit.SPADES_LOW, HalfSuit.SPADES_HIGH -> Suit.SPADES
        HalfSuit.HEARTS_LOW, HalfSuit.HEARTS_HIGH -> Suit.HEARTS
        HalfSuit.DIAMONDS_LOW, HalfSuit.DIAMONDS_HIGH -> Suit.DIAMONDS
        HalfSuit.CLUBS_LOW, HalfSuit.CLUBS_HIGH -> Suit.CLUBS
    }

/** True for the low half (2–7), false for the high half (9–A). */
val HalfSuit.isLow: Boolean get() = name.endsWith("_LOW")
