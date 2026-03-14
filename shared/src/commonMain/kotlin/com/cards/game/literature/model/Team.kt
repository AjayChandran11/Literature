package com.cards.game.literature.model

import kotlinx.serialization.Serializable

@Serializable
data class Team(
    val id: String,
    val name: String,
    val playerIds: List<String>,
    val score: Int = 0
)
