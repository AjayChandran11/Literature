package com.cards.game.literature.bot

import kotlinx.serialization.Serializable

@Serializable
enum class BotDifficulty(
    val label: String,
    val memoryDepth: Int,
    val wrongClaimChance: Float,
    val randomAskChance: Float,
    val missClaimChance: Float,
    val speculativeClaimEnabled: Boolean,
    val prioritizeBlocking: Boolean
) {
    EASY(
        label = "Easy",
        memoryDepth = 8,
        wrongClaimChance = 0.15f,
        randomAskChance = 0.40f,
        missClaimChance = 0.30f,
        speculativeClaimEnabled = false,
        prioritizeBlocking = false
    ),
    MEDIUM(
        label = "Medium",
        memoryDepth = Int.MAX_VALUE,
        wrongClaimChance = 0f,
        randomAskChance = 0f,
        missClaimChance = 0f,
        speculativeClaimEnabled = false,
        prioritizeBlocking = false
    ),
    HARD(
        label = "Hard",
        memoryDepth = Int.MAX_VALUE,
        wrongClaimChance = 0f,
        randomAskChance = 0f,
        missClaimChance = 0f,
        speculativeClaimEnabled = true,
        prioritizeBlocking = true
    )
}
