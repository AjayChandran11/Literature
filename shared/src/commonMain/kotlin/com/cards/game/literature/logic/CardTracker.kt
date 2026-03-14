package com.cards.game.literature.logic

import com.cards.game.literature.model.*

data class CardTrackerState(
    val knownLocations: Map<Card, String> = emptyMap(), // card -> playerId
    val impossibleLocations: Map<Card, Set<String>> = emptyMap() // card -> set of playerIds who definitely don't have it
)

class CardTracker {

    fun buildState(events: List<GameEvent>, players: List<Player>, myId: String): CardTrackerState {
        val known = mutableMapOf<Card, String>()
        val impossible = mutableMapOf<Card, MutableSet<String>>()

        // My own cards are known
        val me = players.find { it.id == myId }
        me?.hand?.forEach { card -> known[card] = myId }

        // Process events to derive knowledge
        for (event in events) {
            when (event) {
                is GameEvent.CardAsked -> {
                    if (event.success) {
                        // Card transferred from target to asker
                        known[event.card] = event.askerId
                    } else {
                        // Target doesn't have the card
                        impossible.getOrPut(event.card) { mutableSetOf() }.add(event.targetId)
                        // Asker doesn't have the card (they asked for it)
                        impossible.getOrPut(event.card) { mutableSetOf() }.add(event.askerId)
                    }
                }
                is GameEvent.DeckClaimed -> {
                    // All cards in this half-suit are accounted for
                    val cards = DeckUtils.getAllCardsForHalfSuit(event.halfSuit)
                    cards.forEach { card ->
                        known.remove(card)
                        impossible.remove(card)
                    }
                }
                else -> { /* no tracking info */ }
            }
        }

        // Remove cards I no longer have from known
        val myCurrentCards = me?.hand?.toSet() ?: emptySet()
        known.entries.removeAll { (card, playerId) ->
            playerId == myId && card !in myCurrentCards
        }

        return CardTrackerState(
            knownLocations = known,
            impossibleLocations = impossible.mapValues { it.value.toSet() }
        )
    }

    fun getKnownCardsForPlayer(state: CardTrackerState, playerId: String): List<Card> {
        return state.knownLocations.filter { it.value == playerId }.keys.toList()
    }

    fun getPossibleHolders(
        state: CardTrackerState,
        card: Card,
        allPlayerIds: List<String>
    ): List<String> {
        // If known, return just that player
        state.knownLocations[card]?.let { return listOf(it) }

        // Otherwise exclude impossible locations and players with no cards
        val impossible = state.impossibleLocations[card] ?: emptySet()
        return allPlayerIds.filter { it !in impossible }
    }
}
