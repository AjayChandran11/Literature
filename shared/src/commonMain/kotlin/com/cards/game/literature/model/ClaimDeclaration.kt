package com.cards.game.literature.model

import kotlinx.serialization.Serializable

@Serializable
data class ClaimDeclaration(
    val claimerId: String,
    val halfSuit: HalfSuit,
    val cardAssignments: Map<String, List<Card>> // playerId -> cards they supposedly hold
)

@Serializable
data class ClaimResult(
    val halfSuit: HalfSuit,
    val claimingTeamId: String,
    val correct: Boolean,
    val awardedToTeamId: String
)

@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
