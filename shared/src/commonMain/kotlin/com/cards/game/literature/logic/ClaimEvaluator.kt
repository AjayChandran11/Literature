package com.cards.game.literature.logic

import com.cards.game.literature.model.ClaimDeclaration
import com.cards.game.literature.model.ClaimResult
import com.cards.game.literature.model.GameState

object ClaimEvaluator {

    fun evaluate(state: GameState, declaration: ClaimDeclaration): ClaimResult {
        val claimerTeam = state.getTeamForPlayer(declaration.claimerId)!!

        // Check if every card assignment is correct
        var allCorrect = true
        for ((playerId, cards) in declaration.cardAssignments) {
            val player = state.getPlayer(playerId) ?: run {
                allCorrect = false
                continue
            }
            for (card in cards) {
                if (card !in player.hand) {
                    allCorrect = false
                    break
                }
            }
            if (!allCorrect) break
        }

        // If correct, claiming team gets the point. If wrong, opposing team gets it.
        val opposingTeam = state.teams.first { it.id != claimerTeam.id }
        val awardedTeamId = if (allCorrect) claimerTeam.id else opposingTeam.id

        return ClaimResult(
            halfSuit = declaration.halfSuit,
            claimingTeamId = claimerTeam.id,
            correct = allCorrect,
            awardedToTeamId = awardedTeamId
        )
    }
}
