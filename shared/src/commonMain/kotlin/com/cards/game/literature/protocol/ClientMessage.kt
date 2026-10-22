package com.cards.game.literature.protocol

import com.cards.game.literature.model.Card
import com.cards.game.literature.model.ClaimDeclaration
import com.cards.game.literature.model.ReactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage {
    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.CreateRoom")
    data class CreateRoom(val playerName: String, val playerCount: Int) : ClientMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.JoinRoom")
    data class JoinRoom(val roomCode: String, val playerName: String) : ClientMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.StartGame")
    data class StartGame(val fillWithBots: Boolean = true, val botDifficulty: String = "MEDIUM") : ClientMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.AskCards")
    data class AskCards(val targetId: String, val cards: List<Card>) : ClientMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.ClaimDeck")
    data class ClaimDeck(val declaration: ClaimDeclaration) : ClientMessage()

    /** Option C (protocol v3+): the claimer's choice of which active teammate
     *  takes the turn after a correct claim emptied their hand. Only valid while
     *  the server has a pending pass selection for this player. */
    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.SelectPassTarget")
    data class SelectPassTarget(val selectedPlayerId: String) : ClientMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.LeaveRoom")
    data object LeaveRoom : ClientMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.Reconnect")
    data class Reconnect(
        val roomCode: String,
        val playerId: String,
        // Proof of identity issued in RoomCreated. Defaulted so messages from
        // legacy clients (protocol v1, no token) still decode on the server.
        val token: String = ""
    ) : ClientMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.LeaveGame")
    data object LeaveGame : ClientMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.SwitchTeam")
    data object SwitchTeam : ClientMessage()

    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.SendReaction")
    data class SendReaction(val reaction: ReactionType) : ClientMessage()

    /** Host-only (protocol v2+): reset a FINISHED room back to the waiting
     *  room with the same connected players for another game. */
    @Serializable
    @SerialName("com.cards.game.literature.protocol.ClientMessage.Rematch")
    data object Rematch : ClientMessage()
}
