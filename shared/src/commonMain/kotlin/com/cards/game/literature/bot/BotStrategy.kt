package com.cards.game.literature.bot

import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.CardTrackerState
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.*

sealed class BotAction {
    data class Ask(val targetId: String, val card: Card) : BotAction()
    data class Claim(val declaration: ClaimDeclaration) : BotAction()
}

class BotStrategy(private val cardTracker: CardTracker = CardTracker()) {

    fun decideMove(state: GameState, botId: String): BotAction {
        val bot = state.getPlayer(botId) ?: error("Bot not found: $botId")
        val trackerState = cardTracker.buildState(state.events, state.players, botId)
        val claimedHalfSuits = state.halfSuitStatuses
            .filter { it.claimedByTeamId != null }
            .map { it.halfSuit }.toSet()

        // Priority 1: Claim if team has all 6 cards of a half-suit confirmed
        val claimAction = tryClaimWithCertainty(state, bot, trackerState, claimedHalfSuits)
        if (claimAction != null) return claimAction

        // Priority 2: Ask for a card we know an opponent has
        val knownAsk = tryAskKnownCard(state, bot, trackerState, claimedHalfSuits)
        if (knownAsk != null) return knownAsk

        // Priority 3: Smart ask — deduce likely opponent for a needed card
        return askSmartOpponent(state, bot, trackerState, claimedHalfSuits)
    }

    private fun tryClaimWithCertainty(
        state: GameState,
        bot: Player,
        trackerState: CardTrackerState,
        claimedHalfSuits: Set<HalfSuit>
    ): BotAction.Claim? {
        val team = state.getTeamForPlayer(bot.id) ?: return null
        val activeTeammates = team.playerIds.filter { pid ->
            state.getPlayer(pid)?.isActive == true
        }.toSet()

        for (halfSuit in HalfSuit.entries) {
            if (halfSuit in claimedHalfSuits) continue

            val cards = DeckUtils.getAllCardsForHalfSuit(halfSuit)
            val assignments = mutableMapOf<String, MutableList<Card>>()

            var allKnown = true
            for (card in cards) {
                val holder = trackerState.knownLocations[card]
                if (holder != null && holder in activeTeammates) {
                    assignments.getOrPut(holder) { mutableListOf() }.add(card)
                } else {
                    allKnown = false
                    break
                }
            }

            if (allKnown && assignments.values.sumOf { it.size } == 6) {
                return BotAction.Claim(
                    ClaimDeclaration(
                        claimerId = bot.id,
                        halfSuit = halfSuit,
                        cardAssignments = assignments.mapValues { it.value.toList() }
                    )
                )
            }
        }
        return null
    }

    private fun tryAskKnownCard(
        state: GameState,
        bot: Player,
        trackerState: CardTrackerState,
        claimedHalfSuits: Set<HalfSuit>
    ): BotAction.Ask? {
        val opponents = state.getOpponents(bot.id).filter { it.isActive }
        if (opponents.isEmpty()) return null

        // Only consider half-suits the bot has cards in AND that aren't claimed
        val myActiveHalfSuits = bot.hand
            .map { DeckUtils.getHalfSuit(it) }
            .filter { it !in claimedHalfSuits }
            .toSet()

        // Shuffle to avoid always asking the same opponent first
        for (opponent in opponents.shuffled()) {
            val knownCards = cardTracker.getKnownCardsForPlayer(trackerState, opponent.id)
            val relevantCards = knownCards.filter { card ->
                DeckUtils.getHalfSuit(card) in myActiveHalfSuits && card !in bot.hand
            }
            if (relevantCards.isNotEmpty()) {
                return BotAction.Ask(targetId = opponent.id, card = relevantCards.random())
            }
        }
        return null
    }

    private fun askSmartOpponent(
        state: GameState,
        bot: Player,
        trackerState: CardTrackerState,
        claimedHalfSuits: Set<HalfSuit>
    ): BotAction {
        val opponents = state.getOpponents(bot.id).filter { it.isActive }
        val botTeam = state.getTeamForPlayer(bot.id)

        // Get unclaimed half-suits the bot has cards in, sorted by how many cards
        // the team already has (prefer half-suits closer to completion)
        val myActiveHalfSuits = bot.hand
            .map { DeckUtils.getHalfSuit(it) }
            .filter { it !in claimedHalfSuits }
            .toSet()

        data class HalfSuitInfo(
            val halfSuit: HalfSuit,
            val missingFromOpponents: List<Card>, // cards we need from opponents
            val teamKnownCount: Int // how many cards the team is known to have
        )

        val halfSuitInfos = myActiveHalfSuits.mapNotNull { halfSuit ->
            val allCards = DeckUtils.getAllCardsForHalfSuit(halfSuit)

            // Count how many cards the team is known to hold
            var teamKnown = 0
            val missingFromOpponents = mutableListOf<Card>()

            for (card in allCards) {
                if (card in bot.hand) {
                    teamKnown++
                    continue
                }
                val holder = trackerState.knownLocations[card]
                if (holder != null && botTeam != null && holder in botTeam.playerIds) {
                    teamKnown++
                    continue
                }
                // Card is either with an opponent or unknown location
                missingFromOpponents.add(card)
            }

            if (missingFromOpponents.isEmpty()) null
            else HalfSuitInfo(halfSuit, missingFromOpponents, teamKnown)
        }.sortedByDescending { it.teamKnownCount } // prioritize half-suits closer to claimable

        for (info in halfSuitInfos) {
            for (card in info.missingFromOpponents.shuffled()) {
                // Check if known to be with a specific opponent
                val knownHolder = trackerState.knownLocations[card]
                if (knownHolder != null && opponents.any { it.id == knownHolder }) {
                    return BotAction.Ask(targetId = knownHolder, card = card)
                }

                // Find possible holders among opponents
                val possibleHolders = cardTracker.getPossibleHolders(
                    trackerState, card, opponents.map { it.id }
                )

                if (possibleHolders.isNotEmpty()) {
                    // Pick a random possible holder to avoid always asking the same player
                    return BotAction.Ask(targetId = possibleHolders.random(), card = card)
                }
            }
        }

        // Fallback: ask a random opponent for any missing card from any unclaimed half-suit
        val opponent = opponents.random()
        for (halfSuit in myActiveHalfSuits) {
            val missingCard = DeckUtils.getAllCardsForHalfSuit(halfSuit)
                .firstOrNull { it !in bot.hand }
            if (missingCard != null) {
                return BotAction.Ask(targetId = opponent.id, card = missingCard)
            }
        }

        // Last resort: shouldn't normally reach here
        val anyHalfSuit = bot.hand.first().let { DeckUtils.getHalfSuit(it) }
        val card = DeckUtils.getAllCardsForHalfSuit(anyHalfSuit).first { it !in bot.hand }
        return BotAction.Ask(targetId = opponent.id, card = card)
    }
}
