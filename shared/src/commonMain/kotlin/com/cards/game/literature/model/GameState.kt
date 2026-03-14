package com.cards.game.literature.model

import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase {
    WAITING,
    IN_PROGRESS,
    FINISHED
}

@Serializable
data class HalfSuitStatus(
    val halfSuit: HalfSuit,
    val claimedByTeamId: String? = null,
    val claimCorrect: Boolean? = null
)

@Serializable
data class GameState(
    val gameId: String,
    val players: List<Player>,
    val teams: List<Team>,
    val currentPlayerIndex: Int = 0,
    val phase: GamePhase = GamePhase.WAITING,
    val halfSuitStatuses: List<HalfSuitStatus> = HalfSuit.entries.map { HalfSuitStatus(it) },
    val events: List<GameEvent> = emptyList(),
    val playerCount: Int = 6
) {
    val currentPlayer: Player get() = players[currentPlayerIndex]
    val isGameOver: Boolean get() = phase == GamePhase.FINISHED

    fun getPlayer(id: String): Player? = players.find { it.id == id }
    fun getTeam(id: String): Team? = teams.find { it.id == id }
    fun getTeamForPlayer(playerId: String): Team? = teams.find { playerId in it.playerIds }
    fun getTeammates(playerId: String): List<Player> {
        val team = getTeamForPlayer(playerId) ?: return emptyList()
        return players.filter { it.id in team.playerIds && it.id != playerId }
    }
    fun getOpponents(playerId: String): List<Player> {
        val team = getTeamForPlayer(playerId) ?: return emptyList()
        return players.filter { it.id !in team.playerIds }
    }
}
