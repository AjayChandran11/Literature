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

        // Priority 1: Claim if team has all 6 cards of a half-suit confirmed
        val claimAction = tryClaimWithCertainty(state, bot, trackerState)
        if (claimAction != null) return claimAction

        // Priority 2: Ask for a known card from an opponent
        val knownAsk = tryAskKnownCard(state, bot, trackerState)
        if (knownAsk != null) return knownAsk

        // Priority 3: Ask a likely opponent for a needed card
        return askLikelyOpponent(state, bot, trackerState)
    }

    private fun tryClaimWithCertainty(
        state: GameState,
        bot: Player,
        trackerState: CardTrackerState
    ): BotAction.Claim? {
        val team = state.getTeamForPlayer(bot.id) ?: return null

        for (halfSuit in HalfSuit.entries) {
            val status = state.halfSuitStatuses.find { it.halfSuit == halfSuit }
            if (status?.claimedByTeamId != null) continue

            val cards = DeckUtils.getAllCardsForHalfSuit(halfSuit)
            val assignments = mutableMapOf<String, MutableList<Card>>()

            var allKnown = true
            for (card in cards) {
                val holder = trackerState.knownLocations[card]
                if (holder != null && holder in team.playerIds) {
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
        trackerState: CardTrackerState
    ): BotAction.Ask? {
        val opponents = state.getOpponents(bot.id).filter { it.isActive }

        // Find cards known to be held by opponents that are in half-suits the bot has cards in
        val myHalfSuits = bot.hand.map { DeckUtils.getHalfSuit(it) }.toSet()

        for (opponent in opponents) {
            val knownCards = cardTracker.getKnownCardsForPlayer(trackerState, opponent.id)
            for (card in knownCards) {
                if (DeckUtils.getHalfSuit(card) in myHalfSuits && card !in bot.hand) {
                    return BotAction.Ask(targetId = opponent.id, card = card)
                }
            }
        }
        return null
    }

    private fun askLikelyOpponent(
        state: GameState,
        bot: Player,
        trackerState: CardTrackerState
    ): BotAction {
        val opponents = state.getOpponents(bot.id).filter { it.isActive }
        val myHalfSuits = bot.hand.map { DeckUtils.getHalfSuit(it) }.toSet()

        // For each half-suit I have cards in, find cards I don't have
        for (halfSuit in myHalfSuits) {
            val status = state.halfSuitStatuses.find { it.halfSuit == halfSuit }
            if (status?.claimedByTeamId != null) continue

            val allCards = DeckUtils.getAllCardsForHalfSuit(halfSuit)
            val missingCards = allCards.filter { it !in bot.hand }

            for (card in missingCards) {
                // Check if this card is known to be with a teammate — skip if so
                val knownHolder = trackerState.knownLocations[card]
                if (knownHolder != null) {
                    val holderTeam = state.getTeamForPlayer(knownHolder)
                    val botTeam = state.getTeamForPlayer(bot.id)
                    if (holderTeam?.id == botTeam?.id) continue // teammate has it
                    // Known to be with an opponent — ask them
                    if (opponents.any { it.id == knownHolder }) {
                        return BotAction.Ask(targetId = knownHolder, card = card)
                    }
                }

                // Ask a random active opponent (prefer those not in impossible list)
                val possibleHolders = cardTracker.getPossibleHolders(
                    trackerState, card, opponents.map { it.id }
                )
                val targetId = possibleHolders.firstOrNull() ?: opponents.firstOrNull()?.id
                if (targetId != null) {
                    return BotAction.Ask(targetId = targetId, card = card)
                }
            }
        }

        // Fallback: ask first opponent for any missing card from any half-suit I have
        val opponent = opponents.first()
        val halfSuit = myHalfSuits.first()
        val card = DeckUtils.getAllCardsForHalfSuit(halfSuit).first { it !in bot.hand }
        return BotAction.Ask(targetId = opponent.id, card = card)
    }
}
