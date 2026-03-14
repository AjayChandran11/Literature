package com.cards.game.literature.protocol

import com.cards.game.literature.model.*
import kotlinx.serialization.Serializable

@Serializable
data class PlayerGameView(
    val myPlayerId: String,
    val myHand: List<Card>,
    val players: List<PublicPlayerInfo>,
    val teams: List<Team>,
    val currentPlayerId: String,
    val phase: GamePhase,
    val halfSuitStatuses: List<HalfSuitStatus>,
    val recentEvents: List<GameEvent>
)

@Serializable
data class PublicPlayerInfo(
    val id: String,
    val name: String,
    val teamId: String,
    val cardCount: Int,
    val isBot: Boolean,
    val isConnected: Boolean = true
)

fun GameState.toPlayerView(playerId: String, connectionStatus: Map<String, Boolean> = emptyMap()): PlayerGameView {
    val myPlayer = getPlayer(playerId)
    return PlayerGameView(
        myPlayerId = playerId,
        myHand = myPlayer?.hand ?: emptyList(),
        players = players.map { player ->
            PublicPlayerInfo(
                id = player.id,
                name = player.name,
                teamId = player.teamId,
                cardCount = player.cardCount,
                isBot = player.isBot,
                isConnected = connectionStatus[player.id] ?: !player.isBot
            )
        },
        teams = teams,
        currentPlayerId = currentPlayer.id,
        phase = phase,
        halfSuitStatuses = halfSuitStatuses,
        recentEvents = events.takeLast(20)
    )
}
