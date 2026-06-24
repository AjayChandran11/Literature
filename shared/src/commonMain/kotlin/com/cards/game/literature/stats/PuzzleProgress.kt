package com.cards.game.literature.stats

import com.cards.game.literature.puzzle.PuzzleKind
import kotlinx.serialization.Serializable

enum class PuzzleStatus { NOT_STARTED, IN_PROGRESS, SOLVED, FAILED }

/**
 * Local progress for the Daily Claim Puzzle: a dedicated streak (independent of the
 * gameplay daily streak in [PlayerStats]) plus today's attempt state. Pure — all
 * transitions return a new instance; persistence lives in PuzzleStore.
 *
 * Streak rules mirror the gameplay streak via the shared [nextDailyStreak]: it advances
 * only on a *solve*, and a missed/failed day breaks it. Up to [MAX_ATTEMPTS] tries per
 * day; fewer tries earns more stars ([starsFor]).
 */
@Serializable
data class PuzzleProgress(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    /** Local epoch-day of the most recent solve; 0 = never. */
    val lastSolvedEpochDay: Long = 0,
    val totalSolved: Int = 0,
    val totalStars: Int = 0,
    /** Legacy one-time how-to flag (pre-variety). Read as "the CLAIM how-to was seen". */
    val howToSeen: Boolean = false,
    /** Per-kind: which puzzle types' how-to explainers the player has seen (one fires per new type). */
    val howToSeenKinds: Set<PuzzleKind> = emptySet(),
    /** Local epoch-day we last flashed the Home "puzzle ready" highlight (a once-per-day nudge). */
    val readyHintShownDay: Long = 0,
    // ── Today's attempt state (valid for [dayEpoch]) ──
    val dayEpoch: Long = 0,
    val attemptsUsed: Int = 0,
    val stars: Int = 0,
    val status: PuzzleStatus = PuzzleStatus.NOT_STARTED
) {
    /** Normalize the day-scoped fields to [today], clearing a stale previous day. */
    fun forDay(today: Long): PuzzleProgress =
        if (dayEpoch == today) this
        else copy(dayEpoch = today, attemptsUsed = 0, stars = 0, status = PuzzleStatus.NOT_STARTED)

    /**
     * Fold one attempt for [today] into progress. No-op once today is already
     * solved/failed (guards re-solving). On a correct attempt the streak advances
     * and stars are awarded by attempt count; on a wrong one the day fails after
     * [MAX_ATTEMPTS].
     */
    fun recordAttempt(today: Long, correct: Boolean): PuzzleProgress {
        val base = forDay(today)
        if (base.status == PuzzleStatus.SOLVED || base.status == PuzzleStatus.FAILED) return base
        val attempts = base.attemptsUsed + 1
        return if (correct) {
            val newStreak = nextDailyStreak(base.lastSolvedEpochDay, today, base.currentStreak)
            base.copy(
                currentStreak = newStreak,
                bestStreak = maxOf(base.bestStreak, newStreak),
                lastSolvedEpochDay = maxOf(base.lastSolvedEpochDay, today),
                totalSolved = base.totalSolved + 1,
                totalStars = base.totalStars + starsFor(attempts),
                attemptsUsed = attempts,
                stars = starsFor(attempts),
                status = PuzzleStatus.SOLVED
            )
        } else {
            base.copy(
                attemptsUsed = attempts,
                status = if (attempts >= MAX_ATTEMPTS) PuzzleStatus.FAILED else PuzzleStatus.IN_PROGRESS
            )
        }
    }

    /**
     * Has the player seen the how-to for [kind]? The legacy [howToSeen] flag counts as the CLAIM
     * how-to (so existing players don't re-see it), and each kind's explainer fires once thereafter.
     */
    fun hasSeenHowTo(kind: PuzzleKind): Boolean =
        kind in howToSeenKinds || (howToSeen && kind == PuzzleKind.CLAIM)

    /** Mark [kind]'s how-to as seen. */
    fun withHowToSeen(kind: PuzzleKind): PuzzleProgress =
        copy(howToSeenKinds = howToSeenKinds + kind)

    /** Streak to show now: alive if solved today, or solved yesterday and today isn't failed. */
    fun displayedStreak(today: Long = currentEpochDay()): Int {
        val s = forDay(today)
        return when {
            lastSolvedEpochDay == 0L -> 0
            lastSolvedEpochDay == today -> currentStreak
            lastSolvedEpochDay == today - 1L && s.status != PuzzleStatus.FAILED -> currentStreak
            else -> 0
        }
    }

    companion object {
        const val MAX_ATTEMPTS = 3
        /** 1st try → 3★, 2nd → 2★, 3rd → 1★. */
        fun starsFor(attempts: Int): Int = (MAX_ATTEMPTS + 1 - attempts).coerceIn(0, MAX_ATTEMPTS)
    }
}
