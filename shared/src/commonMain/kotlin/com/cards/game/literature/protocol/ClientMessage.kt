package com.cards.game.literature.protocol

import com.cards.game.literature.model.Card
import com.cards.game.literature.model.ClaimDeclaration
import kotlinx.serialization.Serializable

@Serializable
sealed class ClientMessage {
    @Serializable
    data class CreateRoom(val playerName: String, val playerCount: Int) : ClientMessage()

    @Serializable
    data class JoinRoom(val roomCode: String, val playerName: String) : ClientMessage()

    @Serializable
    data class StartGame(val fillWithBots: Boolean = true, val botDifficulty: String = "MEDIUM") : ClientMessage()

    @Serializable
    data class AskCards(val targetId: String, val cards: List<Card>) : ClientMessage()

    @Serializable
    data class ClaimDeck(val declaration: ClaimDeclaration) : ClientMessage()

    @Serializable
    data object LeaveRoom : ClientMessage()

    @Serializable
    data class Reconnect(val roomCode: String, val playerId: String) : ClientMessage()

    @Serializable
    data object LeaveGame : ClientMessage()

    @Serializable
    data object SwitchTeam : ClientMessage()
}
