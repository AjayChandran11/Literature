package com.cards.game.literature.model

import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: String,
    val name: String,
    val teamId: String,
    val hand: List<Card> = emptyList(),
    val isBot: Boolean = false
) {
    val cardCount: Int get() = hand.size
    val isActive: Boolean get() = hand.isNotEmpty()
}
