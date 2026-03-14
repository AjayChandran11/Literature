package com.cards.game.literature.logic

import com.cards.game.literature.model.Card

object CardDealer {

    fun dealCards(playerCount: Int): List<List<Card>> {
        val deck = DeckUtils.createFullDeck().shuffled()
        require(48 % playerCount == 0) { "48 cards must divide evenly among $playerCount players" }
        val cardsPerPlayer = 48 / playerCount
        return (0 until playerCount).map { i ->
            deck.subList(i * cardsPerPlayer, (i + 1) * cardsPerPlayer)
        }
    }
}
