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
    val targetPlayerCount: Int,
    /** v4: Quick Play may seat strangers here while the room is still waiting to deal. */
    val isPublic: Boolean = false,
    /** v4: how long until a Quick Play table starts itself, in ms as of this message, or null when
     *  no countdown runs. Relative rather than a deadline so a phone whose clock is off still
     *  counts the right number (emulators and some handsets sit tens of seconds adrift). */
    val autoStartInMs: Long? = null,
)
