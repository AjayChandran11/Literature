package com.cards.game.literature.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReactionType(val emoji: String) {
    THUMBS_UP("\uD83D\uDC4D"),
    CLAP("\uD83D\uDC4F"),
    LAUGH("\uD83D\uDE02"),
    THINK("\uD83E\uDD14"),
    FIRE("\uD83D\uDD25"),
    SAD("\uD83D\uDE22"),
    EYES("\uD83D\uDC40"),
    GG("GG")
}
