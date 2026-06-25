package com.cards.game.literature.puzzle

import kotlin.random.Random

/**
 * Picks which [PuzzleKind] a given day gets — *pseudo-random per day, but balanced*. A fixed
 * 7-slot weekly "bag" (Claim-heavy: it's the richest type) is shuffled deterministically per
 * 7-day block, then indexed within the block. So every week contains exactly the bag's mix in a
 * different, unpredictable order, and no type can dominate a long stretch (which naive day-hashing
 * would allow). Fully deterministic from the epoch-day + [DailyPuzzleGenerator.PUZZLE_SEASON] — no
 * clock, no stored data, "same for everyone".
 */
object PuzzleSchedule {

    /** 2026-06-18 as a local epoch-day → puzzle #1; also anchors the weekly blocks. */
    const val BASE_EPOCH_DAY = 20622L

    /** One week's worth of puzzles. Edit to retune the mix; bump the season when you do. */
    private val WEEK_BAG = listOf(
        PuzzleKind.CLAIM, PuzzleKind.CLAIM, PuzzleKind.CLAIM,
        PuzzleKind.LOCATE, PuzzleKind.LOCATE,
        PuzzleKind.WASTED_ASK, PuzzleKind.WASTED_ASK
    )

    fun kindForDay(epochDay: Long): PuzzleKind {
        val offset = epochDay - BASE_EPOCH_DAY
        val block = offset.floorDiv(WEEK_BAG.size.toLong())
        val indexInBlock = offset.mod(WEEK_BAG.size.toLong()).toInt()
        val rng = Random(block xor (DailyPuzzleGenerator.PUZZLE_SEASON.toLong() * -0x61c8864680b583ebL))
        return WEEK_BAG.shuffled(rng)[indexInBlock]
    }
}
