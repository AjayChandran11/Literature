package com.cards.game.literature.stats

import com.cards.game.literature.model.currentTimeMillis

/**
 * System-zone UTC offset (including DST) in seconds at [epochMillis].
 * Implemented per-platform; the day math below stays pure and testable.
 */
expect fun localUtcOffsetSeconds(epochMillis: Long): Int

/** Local calendar day as a day-number since 1970-01-01 (timezone-aware). */
fun localEpochDay(epochMillis: Long): Long =
    (epochMillis / 1000 + localUtcOffsetSeconds(epochMillis)).floorDiv(86_400L)

/** Today's local calendar day. */
fun currentEpochDay(): Long = localEpochDay(currentTimeMillis())

/**
 * Daily-streak transition. Pure (operates on day-numbers) so it's deterministic to test:
 * - first game ever (lastDay == 0)  -> 1
 * - same day (or out-of-order)      -> unchanged
 * - the very next day               -> +1
 * - a gap of 2+ days                -> reset to 1
 */
fun nextDailyStreak(lastDay: Long, today: Long, current: Int): Int = when {
    lastDay == 0L -> 1
    today <= lastDay -> current
    today == lastDay + 1 -> current + 1
    else -> 1
}
