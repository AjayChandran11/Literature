package com.cards.game.literature.model

import kotlinx.serialization.Serializable

@Serializable
sealed class GameEvent {
    abstract val timestamp: Long

    @Serializable
    data class CardAsked(
        val askerId: String,
        val askerName: String,
        val targetId: String,
        val targetName: String,
        val card: Card,
        val success: Boolean,
        val batchId: String? = null,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    data class DeckClaimed(
        val claimerId: String,
        val claimerName: String,
        val teamId: String,
        val halfSuit: HalfSuit,
        val correct: Boolean,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    data class TurnChanged(
        val newPlayerId: String,
        val newPlayerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    data class GameStarted(
        val playerCount: Int,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    data class GameEnded(
        val winnerTeamId: String?,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    data class TurnTimedOut(
        val playerId: String,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    data class PlayerDisconnected(
        val playerId: String,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    data class PlayerReconnected(
        val playerId: String,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    data class PlayerReplacedByBot(
        val playerId: String,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()
}

expect fun currentTimeMillis(): Long
