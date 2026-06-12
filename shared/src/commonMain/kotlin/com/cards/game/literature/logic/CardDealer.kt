package com.cards.game.literature.logic

import com.cards.game.literature.model.Card
import kotlin.random.Random

object CardDealer {

    /**
     * Deals the 48-card deck evenly among [playerCount] players.
     *
     * The shuffle is seeded so a deal can be reproduced exactly (replays,
     * daily puzzles, debugging). Callers that care about reproducibility
     * generate the seed themselves and record it (see GameState.dealSeed);
     * the default produces a fresh random deal.
     */
    fun dealCards(playerCount: Int, seed: Long = Random.nextLong()): List<List<Card>> {
        val deck = DeckUtils.createFullDeck().shuffled(Random(seed))
        require(48 % playerCount == 0) { "48 cards must divide evenly among $playerCount players" }
        val cardsPerPlayer = 48 / playerCount
        return (0 until playerCount).map { i ->
            deck.subList(i * cardsPerPlayer, (i + 1) * cardsPerPlayer)
        }
    }
}
