package com.cards.game.literature.protocol

import com.cards.game.literature.model.GameEvent
import kotlinx.serialization.Serializable

@Serializable
sealed class ServerMessage {
    @Serializable
    data class RoomCreated(val roomCode: String, val playerId: String) : ServerMessage()

    @Serializable
    data class RoomUpdate(val room: RoomState) : ServerMessage()

    @Serializable
    data class GameStarted(val view: PlayerGameView) : ServerMessage()

    @Serializable
    data class GameUpdate(val view: PlayerGameView) : ServerMessage()

    @Serializable
    data class GameEventOccurred(val event: GameEvent) : ServerMessage()

    @Serializable
    data class Error(val message: String) : ServerMessage()

    @Serializable
    data object RoomClosed : ServerMessage()

    @Serializable
    data class HostTransferred(val newHostId: String, val newHostName: String) : ServerMessage()
}
