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
    val recentEvents: List<GameEvent>,
    /** Server-issued unique id for THIS game; lets the client de-dup stats per match
     *  (a rematch reuses the room code, so the room code alone is not unique). Defaulted
     *  for back-compat: older servers omit it and the client falls back to the room code. */
    val gameId: String = "",
    /** Option C (protocol v3+): non-null while the game is suspended for a claimer
     *  to choose who plays next. The claimer's client shows the picker; everyone
     *  else shows a "choosing…" indicator. Defaulted null so older clients drop it. */
    val pendingPass: PendingPass? = null,
    /** Epoch-ms deadline for the pending pass selection; on timeout the server
     *  auto-picks [PendingPass.defaultTarget]. Null when nothing is pending. */
    val pendingPassDeadlineMs: Long? = null,
    /** Game Variants (protocol v4+): the room's configured per-turn time limit in
     *  seconds, or null for Off (no timer). The client seeds its turn countdown from
     *  this and hides the countdown entirely when null. Defaulted null so older clients
     *  drop it and offline games (which set no timer) show no countdown. */
    val turnTimerSeconds: Int? = null
)

@Serializable
data class PublicPlayerInfo(
    val id: String,
    val name: String,
    val teamId: String,
    val cardCount: Int,
    val isBot: Boolean,
    val isConnected: Boolean = true,
    val isPendingReconnect: Boolean = false,
    val reconnectDeadlineMs: Long? = null
)

fun GameState.toPlayerView(
    playerId: String,
    connectionStatus: Map<String, Boolean> = emptyMap(),
    disconnectDeadlines: Map<String, Long?> = emptyMap(),
    // Server-supplied deadline for an in-flight Option C selection (see GameRoom).
    pendingPassDeadlineMs: Long? = null,
    // The room's per-turn time limit in seconds (see GameRoom.variants); null = Off.
    turnTimerSeconds: Int? = null
): PlayerGameView {
    val myPlayer = getPlayer(playerId)
    return PlayerGameView(
        myPlayerId = playerId,
        myHand = myPlayer?.hand ?: emptyList(),
        players = players.map { player ->
            val deadline = disconnectDeadlines[player.id]
            PublicPlayerInfo(
                id = player.id,
                name = player.name,
                teamId = player.teamId,
                cardCount = player.cardCount,
                isBot = player.isBot,
                isConnected = connectionStatus[player.id] ?: !player.isBot,
                isPendingReconnect = deadline != null,
                reconnectDeadlineMs = deadline
            )
        },
        teams = teams,
        currentPlayerId = currentPlayer.id,
        phase = phase,
        halfSuitStatuses = halfSuitStatuses,
        recentEvents = events.takeLast(20),
        gameId = this.gameId,
        pendingPass = this.pendingPass,
        pendingPassDeadlineMs = if (this.pendingPass != null) pendingPassDeadlineMs else null,
        turnTimerSeconds = turnTimerSeconds
    )
}
