package com.cards.game.literature.puzzle

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Proof harness (JVM only, not shipped): sweeps many seeds to confirm each puzzle kind has a
 * healthy hit-rate — so the on-device forward-search terminates in a few tries — and that every
 * real calendar day (its scheduled kind + production seed) lands a puzzle within the cap.
 */
class DailyPuzzleSeedSweepTest {

    /** Mirror of DailyPuzzleRepository.seedForDay (its stride is private). */
    private fun seedForDay(epochDay: Long): Long = epochDay * 1_000_003L

    @Test
    fun healthyHitRatePerKind() {
        val n = 5000
        for (kind in PuzzleKind.entries) {
            var hits = 0
            for (seed in 0L until n) if (DailyPuzzleGenerator.generate(kind, seed) != null) hits++
            val rate = hits.toDouble() / n
            assertTrue(rate > 0.10, "kind=$kind hit rate $rate too low — on-device forward-search would be slow")
        }
    }

    @Test
    fun everyScheduledDayResolvesWithinCap() {
        // ~5.5 years of days, each with its scheduled kind and the production seed.
        for (day in 0L until 2000L) {
            val epochDay = PuzzleSchedule.BASE_EPOCH_DAY + day
            val kind = PuzzleSchedule.kindForDay(epochDay)
            assertTrue(
                DailyPuzzleGenerator.generateForDay(kind, seedForDay(epochDay)) != null,
                "no puzzle for day=$day kind=$kind"
            )
        }
    }
}
