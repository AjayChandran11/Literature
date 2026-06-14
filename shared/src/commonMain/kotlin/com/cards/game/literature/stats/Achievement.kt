package com.cards.game.literature.stats

import kotlinx.serialization.Serializable

/**
 * Achievement identifiers. Display names/descriptions are UI-layer string
 * resources keyed by this enum — the engine only deals in IDs.
 *
 * Unlocks are persisted as a Map<String, Long> (name -> unlock timestamp)
 * rather than enum keys so removing an entry can never break decoding.
 */
@Serializable
enum class Achievement {
    /** Win your first game. */
    FIRST_WIN,

    /** Reach a 3-game win streak. */
    HAT_TRICK,

    /** Reach a 5-game win streak. */
    ON_FIRE,

    /** Play 25 games. */
    VETERAN,

    /** Play 100 games. */
    CENTURION,

    /** Play your first online game. */
    SOCIALITE,

    /** Win 10 online games. */
    ONLINE_CHAMP,

    /** Win a game against HARD bots. */
    BOT_SLAYER,

    /** Make 10 correct claims overall. */
    SHARP_CALLER,

    /** Win a game in which every ask succeeded (minimum 5 asks). */
    PERFECT_GAME,

    /** Make 3 correct claims in a single game. */
    CLAIM_MASTER
}

object AchievementEvaluator {

    /**
     * Returns every achievement whose condition holds after this game.
     * Pure and idempotent — the caller subtracts already-unlocked entries.
     *
     * [stats] is the aggregate AFTER folding in [record].
     */
    fun satisfiedBy(stats: PlayerStats, record: MatchRecord): Set<Achievement> = buildSet {
        if (stats.wins >= 1) add(Achievement.FIRST_WIN)
        if (stats.currentStreak >= 3) add(Achievement.HAT_TRICK)
        if (stats.currentStreak >= 5) add(Achievement.ON_FIRE)
        if (stats.gamesPlayed >= 25) add(Achievement.VETERAN)
        if (stats.gamesPlayed >= 100) add(Achievement.CENTURION)
        if (stats.onlineGames >= 1) add(Achievement.SOCIALITE)
        if (stats.onlineWins >= 10) add(Achievement.ONLINE_CHAMP)
        if ((stats.winsByDifficulty["HARD"] ?: 0) >= 1) add(Achievement.BOT_SLAYER)
        if (stats.correctClaims >= 10) add(Achievement.SHARP_CALLER)
        if (record.outcome == Outcome.WIN && record.myAsks >= 5 &&
            record.myAsksSuccessful == record.myAsks
        ) add(Achievement.PERFECT_GAME)
        if (record.myClaimsCorrect >= 3) add(Achievement.CLAIM_MASTER)
    }
}
