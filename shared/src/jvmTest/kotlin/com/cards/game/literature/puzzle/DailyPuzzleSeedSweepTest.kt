package com.cards.game.literature.puzzle

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Proof harness (JVM only, not shipped): sweeps many seeds to confirm the generator
 * has a healthy hit-rate — so the on-device forward-search terminates in a few tries —
 * and that every day's search lands a puzzle within the cap.
 */
class DailyPuzzleSeedSweepTest {

    @Test
    fun healthyHitRate() {
        val n = 5000
        var hits = 0
        for (seed in 0L until n) if (DailyPuzzleGenerator.generate(seed) != null) hits++
        val rate = hits.toDouble() / n
        assertTrue(rate > 0.10, "hit rate $rate too low — on-device forward-search would be slow")
    }

    @Test
    fun everyDayResolvesWithinCap() {
        for (day in 0L until 1000L) {
            assertTrue(DailyPuzzleGenerator.generateForDay(day * 7919L) != null, "no puzzle for day=$day")
        }
    }
}
