package com.cards.game.literature.protocol

import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.ReactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ServerMessage {
    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.RoomCreated")
    data class RoomCreated(
        val roomCode: String,
        val playerId: String,
        // Defaulted so messages from old servers still decode; old clients
        // ignore the extra field (ignoreUnknownKeys). See Protocol.VERSION.
        val protocolVersion: Int = 1,
        // Secret the client must present in Reconnect to prove identity —
        // prevents session hijack via guessable room code + sequential IDs.
        val reconnectToken: String = ""
    ) : ServerMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.RoomUpdate")
    data class RoomUpdate(val room: RoomState) : ServerMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.GameStarted")
    data class GameStarted(val view: PlayerGameView) : ServerMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.GameUpdate")
    data class GameUpdate(val view: PlayerGameView) : ServerMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.GameEventOccurred")
    data class GameEventOccurred(val event: GameEvent) : ServerMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.Error")
    data class Error(val message: String) : ServerMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.RoomClosed")
    data object RoomClosed : ServerMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.HostTransferred")
    data class HostTransferred(val newHostId: String, val newHostName: String) : ServerMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.ReactionReceived")
    data class ReactionReceived(
        val senderId: String,
        val senderName: String,
        val reaction: ReactionType
    ) : ServerMessage()

    /** Sent only to protocol v2+ sessions (old clients can't decode unknown
     *  message types); v1 clients still receive the accompanying RoomUpdate. */
    @Serializable
    @SerialName("com.cards.game.literature.protocol.ServerMessage.RematchStarted")
    data class RematchStarted(val room: RoomState) : ServerMessage()
}
