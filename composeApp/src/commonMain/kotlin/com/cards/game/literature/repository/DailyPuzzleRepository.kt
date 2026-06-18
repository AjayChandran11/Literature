package com.cards.game.literature.repository

import com.cards.game.literature.puzzle.DailyPuzzle
import com.cards.game.literature.puzzle.DailyPuzzleGenerator
import com.cards.game.literature.stats.currentEpochDay

/**
 * Supplies the deterministic Daily Claim Puzzle for a calendar day. The day's seed is
 * spread far apart per day (so adjacent days never share a puzzle), then the generator's
 * forward-search lands the first solvable seed in that day's block. Today's puzzle is
 * cached for the process so it's generated once.
 */
class DailyPuzzleRepository {
    private var cachedDay: Long = Long.MIN_VALUE
    private var cached: DailyPuzzle? = null

    /** Human-facing puzzle number (#1 on launch day). */
    fun puzzleNumber(epochDay: Long = currentEpochDay()): Int =
        (epochDay - BASE_EPOCH_DAY + 1L).coerceAtLeast(1L).toInt()

    /** Today's puzzle, or null only if the forward-search somehow exhausts its cap. */
    fun todaysPuzzle(epochDay: Long = currentEpochDay()): DailyPuzzle? {
        if (epochDay != cachedDay) {
            cached = DailyPuzzleGenerator.generateForDay(seedForDay(epochDay))
            cachedDay = epochDay
        }
        return cached
    }

    private fun seedForDay(epochDay: Long): Long = epochDay * SEED_STRIDE

    companion object {
        /** 2026-06-18 as a local epoch-day → puzzle #1. */
        const val BASE_EPOCH_DAY = 20622L
        /** Per-day seed spacing; far larger than the forward-search cap so days never collide. */
        private const val SEED_STRIDE = 1_000_003L
    }
}
