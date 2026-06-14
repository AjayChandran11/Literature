package com.cards.game.literature.stats

import kotlinx.serialization.Serializable

/**
 * Lifetime aggregate statistics for the local player.
 * Updated via [applying]; persisted client-side as JSON.
 */
@Serializable
data class PlayerStats(
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val onlineGames: Int = 0,
    val onlineWins: Int = 0,
    /** Consecutive wins; any loss or draw resets it. */
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    /** Local epoch-day of the most recent completed game; 0 = never. */
    val lastPlayedEpochDay: Long = 0,
    /** Consecutive calendar days with at least one completed game. */
    val currentDailyStreak: Int = 0,
    val bestDailyStreak: Int = 0,
    val totalAsks: Int = 0,
    val successfulAsks: Int = 0,
    val totalClaims: Int = 0,
    val correctClaims: Int = 0,
    /** Offline games per bot difficulty name (EASY/MEDIUM/HARD). */
    val gamesByDifficulty: Map<String, Int> = emptyMap(),
    val winsByDifficulty: Map<String, Int> = emptyMap()
) {
    val winRate: Float get() = if (gamesPlayed == 0) 0f else wins.toFloat() / gamesPlayed
    val askSuccessRate: Float get() = if (totalAsks == 0) 0f else successfulAsks.toFloat() / totalAsks
    val claimAccuracy: Float get() = if (totalClaims == 0) 0f else correctClaims.toFloat() / totalClaims

    /**
     * Streak to show right now: alive only if the last game was today or yesterday.
     * Missing a full day breaks it (0) even before the next game is played.
     */
    fun displayedDailyStreak(today: Long = currentEpochDay()): Int =
        if (lastPlayedEpochDay != 0L && today - lastPlayedEpochDay <= 1) currentDailyStreak else 0

    /** Returns a new [PlayerStats] with [record] folded in. Pure — no I/O. */
    fun applying(record: MatchRecord): PlayerStats {
        val isWin = record.outcome == Outcome.WIN
        val newStreak = if (isWin) currentStreak + 1 else 0
        val today = localEpochDay(record.timestamp)
        val newDailyStreak = nextDailyStreak(lastPlayedEpochDay, today, currentDailyStreak)
        val difficulty = record.botDifficulty
        return copy(
            gamesPlayed = gamesPlayed + 1,
            wins = wins + if (isWin) 1 else 0,
            losses = losses + if (record.outcome == Outcome.LOSS) 1 else 0,
            draws = draws + if (record.outcome == Outcome.DRAW) 1 else 0,
            onlineGames = onlineGames + if (record.isOnline) 1 else 0,
            onlineWins = onlineWins + if (record.isOnline && isWin) 1 else 0,
            currentStreak = newStreak,
            bestStreak = maxOf(bestStreak, newStreak),
            lastPlayedEpochDay = maxOf(lastPlayedEpochDay, today),
            currentDailyStreak = newDailyStreak,
            bestDailyStreak = maxOf(bestDailyStreak, newDailyStreak),
            totalAsks = totalAsks + record.myAsks,
            successfulAsks = successfulAsks + record.myAsksSuccessful,
            totalClaims = totalClaims + record.myClaims,
            correctClaims = correctClaims + record.myClaimsCorrect,
            gamesByDifficulty = if (difficulty != null) {
                gamesByDifficulty + (difficulty to (gamesByDifficulty[difficulty] ?: 0) + 1)
            } else gamesByDifficulty,
            winsByDifficulty = if (difficulty != null && isWin) {
                winsByDifficulty + (difficulty to (winsByDifficulty[difficulty] ?: 0) + 1)
            } else winsByDifficulty
        )
    }
}
