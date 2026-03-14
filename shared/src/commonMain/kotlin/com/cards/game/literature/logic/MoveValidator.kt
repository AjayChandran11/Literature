package com.cards.game.literature.logic

import com.cards.game.literature.model.*

object MoveValidator {

    fun validateAsk(
        state: GameState,
        askerId: String,
        targetId: String,
        card: Card
    ): ValidationResult {
        val asker = state.getPlayer(askerId)
            ?: return ValidationResult(false, "Asker not found")
        val target = state.getPlayer(targetId)
            ?: return ValidationResult(false, "Target not found")

        if (state.currentPlayer.id != askerId) {
            return ValidationResult(false, "It's not your turn")
        }
        if (askerId == targetId) {
            return ValidationResult(false, "Cannot ask yourself")
        }
        // Must ask an opponent
        val askerTeam = state.getTeamForPlayer(askerId)
        val targetTeam = state.getTeamForPlayer(targetId)
        if (askerTeam?.id == targetTeam?.id) {
            return ValidationResult(false, "Cannot ask a teammate")
        }
        // Target must have cards
        if (!target.isActive) {
            return ValidationResult(false, "${target.name} has no cards")
        }
        // Asker must not hold the card they're asking for
        if (card in asker.hand) {
            return ValidationResult(false, "You already have that card")
        }
        // Asker must hold at least one card from the same half-suit
        val halfSuit = DeckUtils.getHalfSuit(card)
        val hasCardFromSameHalfSuit = asker.hand.any { DeckUtils.getHalfSuit(it) == halfSuit }
        if (!hasCardFromSameHalfSuit) {
            return ValidationResult(false, "You must hold a card from the same half-suit")
        }
        // The half-suit must not already be claimed
        val status = state.halfSuitStatuses.find { it.halfSuit == halfSuit }
        if (status?.claimedByTeamId != null) {
            return ValidationResult(false, "This half-suit has already been claimed")
        }

        return ValidationResult(true)
    }

    fun validateClaim(
        state: GameState,
        declaration: ClaimDeclaration
    ): ValidationResult {
        val claimer = state.getPlayer(declaration.claimerId)
            ?: return ValidationResult(false, "Claimer not found")

        if (state.currentPlayer.id != declaration.claimerId) {
            return ValidationResult(false, "It's not your turn")
        }

        val claimerTeam = state.getTeamForPlayer(declaration.claimerId)
            ?: return ValidationResult(false, "Claimer has no team")

        // All assigned players must be on the claimer's team
        for (playerId in declaration.cardAssignments.keys) {
            if (playerId !in claimerTeam.playerIds) {
                return ValidationResult(false, "Can only assign cards to teammates")
            }
        }

        // All assigned cards must belong to the declared half-suit
        val expectedCards = DeckUtils.getAllCardsForHalfSuit(declaration.halfSuit).toSet()
        val assignedCards = declaration.cardAssignments.values.flatten()
        for (card in assignedCards) {
            if (card !in expectedCards) {
                return ValidationResult(false, "${card.displayName} is not in ${declaration.halfSuit.displayName}")
            }
        }

        // Must assign exactly 6 cards
        if (assignedCards.size != 6) {
            return ValidationResult(false, "Must assign exactly 6 cards")
        }

        // No duplicate assignments
        if (assignedCards.toSet().size != 6) {
            return ValidationResult(false, "Duplicate card assignments")
        }

        // Must assign all 6 cards of the half-suit
        if (assignedCards.toSet() != expectedCards) {
            return ValidationResult(false, "Must assign all cards from the half-suit")
        }

        // Half-suit must not already be claimed
        val status = state.halfSuitStatuses.find { it.halfSuit == declaration.halfSuit }
        if (status?.claimedByTeamId != null) {
            return ValidationResult(false, "This half-suit has already been claimed")
        }

        return ValidationResult(true)
    }
}
