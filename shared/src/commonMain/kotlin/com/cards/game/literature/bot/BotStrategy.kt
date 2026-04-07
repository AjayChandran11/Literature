package com.cards.game.literature.bot

import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.CardTrackerState
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.*
import kotlin.random.Random

sealed class BotAction {
    data class Ask(val targetId: String, val card: Card) : BotAction()
    data class Claim(val declaration: ClaimDeclaration) : BotAction()
}

class BotStrategy(private val cardTracker: CardTracker = CardTracker()) {

    fun decideMove(state: GameState, botId: String, difficulty: BotDifficulty = BotDifficulty.MEDIUM): BotAction {
        val bot = state.getPlayer(botId) ?: error("Bot not found: $botId")

        // Apply memory depth limitation — Easy bots only remember recent events
        val effectiveEvents = if (difficulty.memoryDepth < Int.MAX_VALUE) {
            state.events.takeLast(difficulty.memoryDepth)
        } else {
            state.events
        }
        val trackerState = cardTracker.buildState(effectiveEvents, state.players, botId)
        val claimedHalfSuits = state.halfSuitStatuses
            .filter { it.claimedByTeamId != null }
            .map { it.halfSuit }.toSet()

        // Priority 1: Claim if team has all 6 cards of a half-suit confirmed
        val claimAction = tryClaimWithCertainty(state, bot, trackerState, claimedHalfSuits, difficulty)
        if (claimAction != null) return claimAction

        // Priority 1.5 (Hard only): Speculative claim with 5/6 known via elimination
        if (difficulty.speculativeClaimEnabled) {
            val speculativeClaim = trySpeculativeClaim(state, bot, trackerState, claimedHalfSuits)
            if (speculativeClaim != null) return speculativeClaim
        }

        val opponents = state.getOpponents(bot.id).filter { it.isActive }
        val myActiveHalfSuits = bot.hand
            .map { DeckUtils.getHalfSuit(it) }
            .filter { it !in claimedHalfSuits }
            .toSet()

        // Easy bots: single random check here — 40% of the time skip smart logic entirely
        if (Random.nextFloat() < difficulty.randomAskChance) {
            val randomAsk = askRandomOpponent(bot, opponents, myActiveHalfSuits)
            if (randomAsk != null) return randomAsk
            // null only if no active opponents — fall through to smart logic
        }

        // Priority 2: Ask for a card we know an opponent has
        val knownAsk = tryAskKnownCard(bot, opponents, trackerState, myActiveHalfSuits)
        if (knownAsk != null) return knownAsk

        // Priority 3: Smart ask — deduce likely opponent for a needed card
        return askSmartOpponent(state, bot, opponents, trackerState, claimedHalfSuits, myActiveHalfSuits, difficulty)
    }

    private fun tryClaimWithCertainty(
        state: GameState,
        bot: Player,
        trackerState: CardTrackerState,
        claimedHalfSuits: Set<HalfSuit>,
        difficulty: BotDifficulty
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
                // Easy bots may miss a claimable half-suit
                if (Random.nextFloat() < difficulty.missClaimChance) continue

                // Easy bots may make a wrong claim assignment
                val finalAssignments = if (Random.nextFloat() < difficulty.wrongClaimChance) {
                    corruptClaimAssignment(assignments, activeTeammates)
                } else {
                    assignments.mapValues { it.value.toList() }
                }

                return BotAction.Claim(
                    ClaimDeclaration(
                        claimerId = bot.id,
                        halfSuit = halfSuit,
                        cardAssignments = finalAssignments
                    )
                )
            }
        }
        return null
    }

    /**
     * Hard-only: Attempt a claim when 5 of 6 cards in a half-suit are known to be on the team
     * and the 6th card can be confidently deduced to also be with a teammate via elimination.
     *
     * Only speculates when:
     * - Exactly 5 cards are confirmed with teammates
     * - The 6th card is NOT known to be with an opponent
     * - Elimination rules out ALL opponents for that card (so it must be with a teammate)
     * - Exactly one candidate teammate remains after elimination
     */
    private fun trySpeculativeClaim(
        state: GameState,
        bot: Player,
        trackerState: CardTrackerState,
        claimedHalfSuits: Set<HalfSuit>
    ): BotAction.Claim? {
        val team = state.getTeamForPlayer(bot.id) ?: return null
        val activeTeammates = team.playerIds.filter { pid ->
            state.getPlayer(pid)?.isActive == true
        }.toSet()
        val activeOpponents = state.getOpponents(bot.id)
            .filter { it.isActive }
            .map { it.id }.toSet()

        for (halfSuit in HalfSuit.entries) {
            if (halfSuit in claimedHalfSuits) continue

            val cards = DeckUtils.getAllCardsForHalfSuit(halfSuit)
            val assignments = mutableMapOf<String, MutableList<Card>>()
            var unknownCard: Card? = null
            var hasOpponentCard = false

            for (card in cards) {
                val holder = trackerState.knownLocations[card]
                when {
                    holder != null && holder in activeTeammates -> {
                        assignments.getOrPut(holder) { mutableListOf() }.add(card)
                    }
                    holder != null && holder in activeOpponents -> {
                        // Card is confirmed with an opponent — can't claim this half-suit
                        hasOpponentCard = true
                        break
                    }
                    unknownCard == null -> {
                        // First unknown/unlocated card — potential guess target
                        unknownCard = card
                    }
                    else -> {
                        // More than 1 unknown — too risky
                        unknownCard = null
                        break
                    }
                }
            }

            if (hasOpponentCard) continue

            // Exactly 5 known on team + 1 unknown (not with any opponent)
            if (unknownCard != null && assignments.values.sumOf { it.size } == 5) {
                // Check if elimination rules out ALL opponents for this card
                val impossibleFor = trackerState.impossibleLocations[unknownCard] ?: emptySet()
                val possibleOpponents = activeOpponents.filter { it !in impossibleFor }

                // Only speculate if no opponent could have it (card must be with a teammate)
                if (possibleOpponents.isNotEmpty()) continue

                // Now find which teammate has it via elimination
                val candidates = activeTeammates.filter { it !in impossibleFor }

                if (candidates.size == 1) {
                    // Only one possible teammate — high-confidence guess
                    val holder = candidates.first()
                    assignments.getOrPut(holder) { mutableListOf() }.add(unknownCard)

                    return BotAction.Claim(
                        ClaimDeclaration(
                            claimerId = bot.id,
                            halfSuit = halfSuit,
                            cardAssignments = assignments.mapValues { it.value.toList() }
                        )
                    )
                }
                // Multiple teammate candidates — too uncertain, skip
            }
        }
        return null
    }

    private fun tryAskKnownCard(
        bot: Player,
        opponents: List<Player>,
        trackerState: CardTrackerState,
        myActiveHalfSuits: Set<HalfSuit>
    ): BotAction.Ask? {
        if (opponents.isEmpty()) return null

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
        opponents: List<Player>,
        trackerState: CardTrackerState,
        claimedHalfSuits: Set<HalfSuit>,
        myActiveHalfSuits: Set<HalfSuit>,
        difficulty: BotDifficulty
    ): BotAction {
        val botTeam = state.getTeamForPlayer(bot.id)

        // Get unclaimed half-suits the bot has cards in, sorted by how many cards
        // the team already has (prefer half-suits closer to completion)

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

        // Hard mode: prioritize blocking opponent near-complete half-suits
        val effectiveInfos = if (difficulty.prioritizeBlocking) {
            val blockingHalfSuits = findBlockingTargets(state, trackerState, bot, claimedHalfSuits, myActiveHalfSuits)
            if (blockingHalfSuits.isNotEmpty()) {
                // Put blocking half-suits first, then the rest
                val blocking = halfSuitInfos.filter { it.halfSuit in blockingHalfSuits }
                val rest = halfSuitInfos.filter { it.halfSuit !in blockingHalfSuits }
                blocking + rest
            } else halfSuitInfos
        } else halfSuitInfos

        for (info in effectiveInfos) {
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
                    val target = if (difficulty.prioritizeBlocking) {
                        // Hard: pick opponent with the most cards (more likely to hold it)
                        possibleHolders.maxByOrNull { pid ->
                            state.getPlayer(pid)?.cardCount ?: 0
                        } ?: possibleHolders.random()
                    } else {
                        possibleHolders.random()
                    }
                    return BotAction.Ask(targetId = target, card = card)
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

    /**
     * Hard mode: find half-suits where opponents are close to claiming (4+ cards known with them).
     * Returns the set of half-suits that the bot should prioritize disrupting.
     */
    private fun findBlockingTargets(
        state: GameState,
        trackerState: CardTrackerState,
        bot: Player,
        claimedHalfSuits: Set<HalfSuit>,
        myActiveHalfSuits: Set<HalfSuit>
    ): Set<HalfSuit> {
        val botTeam = state.getTeamForPlayer(bot.id) ?: return emptySet()
        val opponentIds = state.players
            .filter { it.id !in botTeam.playerIds && it.isActive }
            .map { it.id }.toSet()

        return myActiveHalfSuits.filter { halfSuit ->
            if (halfSuit in claimedHalfSuits) return@filter false
            val allCards = DeckUtils.getAllCardsForHalfSuit(halfSuit)

            var opponentKnown = 0
            for (card in allCards) {
                val holder = trackerState.knownLocations[card]
                if (holder != null && holder in opponentIds) {
                    opponentKnown++
                }
            }

            // Flag as blocking target if opponents have 4+ cards known
            opponentKnown >= 4
        }.toSet()
    }

    /**
     * Easy mode helper: pick a random opponent and ask for a random missing card.
     */
    private fun askRandomOpponent(
        bot: Player,
        opponents: List<Player>,
        myActiveHalfSuits: Set<HalfSuit>
    ): BotAction.Ask? {
        if (opponents.isEmpty()) return null
        val opponent = opponents.random()
        for (halfSuit in myActiveHalfSuits.shuffled()) {
            val missingCard = DeckUtils.getAllCardsForHalfSuit(halfSuit)
                .filter { it !in bot.hand }
                .randomOrNull()
            if (missingCard != null) {
                return BotAction.Ask(targetId = opponent.id, card = missingCard)
            }
        }
        return null
    }

    /**
     * Corrupts a correct claim assignment by swapping one card between two teammates.
     * This makes Easy bots occasionally make wrong claims.
     */
    private fun corruptClaimAssignment(
        assignments: Map<String, MutableList<Card>>,
        activeTeammates: Set<String>
    ): Map<String, List<Card>> {
        val result = assignments.mapValues { it.value.toMutableList() }.toMutableMap()
        val playersWithCards = result.filter { it.value.isNotEmpty() }.keys.toList()

        if (playersWithCards.size >= 2) {
            // Swap one card between two random teammates
            val player1 = playersWithCards.random()
            val player2 = playersWithCards.filter { it != player1 }.random()
            val card = result[player1]!!.random()
            result[player1]!!.remove(card)
            result[player2]!!.add(card)
        } else if (playersWithCards.size == 1 && activeTeammates.size >= 2) {
            // All cards with one player — move one to a random other teammate
            val holder = playersWithCards.first()
            val other = activeTeammates.filter { it != holder }.random()
            val card = result[holder]!!.random()
            result[holder]!!.remove(card)
            result.getOrPut(other) { mutableListOf() }.add(card)
        }

        return result.mapValues { it.value.toList() }
    }
}
