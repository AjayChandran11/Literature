package com.cards.game.literature.stats

import kotlinx.serialization.Serializable

@Serializable
enum class Outcome { WIN, LOSS, DRAW }

/**
 * One finished game from the local player's perspective.
 * Persisted client-side only (match history).
 */
@Serializable
data class MatchRecord(
    val timestamp: Long,
    val isOnline: Boolean,
    val playerCount: Int,
    /** Bot difficulty name for offline games; null online (host's choice, unknown here). */
    val botDifficulty: String? = null,
    val myScore: Int,
    val opponentScore: Int,
    val outcome: Outcome,
    val myAsks: Int = 0,
    val myAsksSuccessful: Int = 0,
    val myClaims: Int = 0,
    val myClaimsCorrect: Int = 0
)
