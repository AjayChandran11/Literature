package com.cards.game.literature.protocol

import kotlinx.serialization.Serializable

@Serializable
enum class RoomPhase {
    WAITING,
    IN_PROGRESS,
    FINISHED
}

@Serializable
data class RoomPlayerInfo(
    val id: String,
    val name: String,
    val teamId: String,
    val isBot: Boolean,
    val isConnected: Boolean,
    val isHost: Boolean
)

@Serializable
data class RoomState(
    val roomCode: String,
    val phase: RoomPhase,
    val players: List<RoomPlayerInfo>,
    val hostPlayerId: String,
    val targetPlayerCount: Int
)
