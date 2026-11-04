package com.cards.game.literature.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class GameEvent {
    abstract val timestamp: Long

    @Serializable
    @SerialName("com.cards.game.literature.model.GameEvent.CardAsked")
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
    @SerialName("com.cards.game.literature.model.GameEvent.DeckClaimed")
    data class DeckClaimed(
        val claimerId: String,
        val claimerName: String,
        val teamId: String,
        val halfSuit: HalfSuit,
        val correct: Boolean,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    @SerialName("com.cards.game.literature.model.GameEvent.TurnChanged")
    data class TurnChanged(
        val newPlayerId: String,
        val newPlayerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    @SerialName("com.cards.game.literature.model.GameEvent.GameStarted")
    data class GameStarted(
        val playerCount: Int,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    @SerialName("com.cards.game.literature.model.GameEvent.GameEnded")
    data class GameEnded(
        val winnerTeamId: String?,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    @SerialName("com.cards.game.literature.model.GameEvent.TurnTimedOut")
    data class TurnTimedOut(
        val playerId: String,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    @SerialName("com.cards.game.literature.model.GameEvent.PlayerDisconnected")
    data class PlayerDisconnected(
        val playerId: String,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    /** v4: someone arrived by code or invite link and took over [replacedName]'s seat at a
     *  running table. Clients before v4 receive a [PlayerReconnected] in its place. */
    @Serializable
    @SerialName("com.cards.game.literature.model.GameEvent.PlayerJoined")
    data class PlayerJoined(
        val playerId: String,
        val playerName: String,
        val replacedName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    @SerialName("com.cards.game.literature.model.GameEvent.PlayerReconnected")
    data class PlayerReconnected(
        val playerId: String,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()

    @Serializable
    @SerialName("com.cards.game.literature.model.GameEvent.PlayerReplacedByBot")
    data class PlayerReplacedByBot(
        val playerId: String,
        val playerName: String,
        override val timestamp: Long = currentTimeMillis()
    ) : GameEvent()
}

expect fun currentTimeMillis(): Long
