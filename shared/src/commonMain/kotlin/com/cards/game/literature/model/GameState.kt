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

/**
 * Set when a correct claim empties the claimer's hand and 2+ of their teammates
 * are still active — the game is suspended so the claimer chooses who takes the
 * next turn (Option C). Null the rest of the time. Never contains the claimer.
 * Resolved via [com.cards.game.literature.logic.GameEngine.applyPassSelection];
 * [defaultTarget] is the fallback used for bots, legacy clients, or a timeout
 * and equals the old deterministic "first active teammate" behaviour.
 */
@Serializable
data class PendingPass(
    val claimerId: String,
    val eligibleTeammateIds: List<String>
) {
    val defaultTarget: String get() = eligibleTeammateIds.first()
}

@Serializable
data class GameState(
    val gameId: String,
    val players: List<Player>,
    val teams: List<Team>,
    val currentPlayerIndex: Int = 0,
    val phase: GamePhase = GamePhase.WAITING,
    val halfSuitStatuses: List<HalfSuitStatus> = HalfSuit.entries.map { HalfSuitStatus(it) },
    val events: List<GameEvent> = emptyList(),
    val playerCount: Int = 6,
    // Seed used for the deal — makes the game reproducible (replays, puzzles).
    // SECURITY: never expose this to clients mid-game; the seed reconstructs
    // every hand. It stays out of PlayerGameView; GameState itself is never
    // sent over the wire in online mode.
    val dealSeed: Long = 0L,
    // Non-null while a correct claim has suspended the game for the claimer to
    // pick who plays next (Option C). Defaulted so older serialized states and
    // wire views decode unchanged.
    val pendingPass: PendingPass? = null,
    // Epoch-ms deadline for the pending pass selection, for a client countdown.
    // Set only on the CLIENT's reconstructed state (from PlayerGameView); the
    // server owns the real timer and sends the deadline via the view.
    val pendingPassDeadlineMs: Long? = null
) {
    val currentPlayer: Player get() = players[currentPlayerIndex]
    val isGameOver: Boolean get() = phase == GamePhase.FINISHED
    val isAwaitingPassSelection: Boolean get() = pendingPass != null

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
