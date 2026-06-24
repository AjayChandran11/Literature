package com.cards.game.literature.puzzle

import kotlin.test.Test
import kotlin.test.assertEquals

class PuzzleScheduleTest {

    @Test
    fun kindForDayIsDeterministic() {
        for (day in 0L until 100L) {
            val e = PuzzleSchedule.BASE_EPOCH_DAY + day
            assertEquals(PuzzleSchedule.kindForDay(e), PuzzleSchedule.kindForDay(e), "day offset=$day")
        }
    }

    @Test
    fun everyWeekHoldsTheBagMix() {
        // Each aligned 7-day block contains exactly the weekly bag: 3 CLAIM, 2 LOCATE, 2 WASTED_ASK —
        // so no type can dominate a stretch, and the order varies block to block (pseudo-random).
        for (block in 0L until 30L) {
            val start = PuzzleSchedule.BASE_EPOCH_DAY + block * 7
            val kinds = (0L until 7L).map { PuzzleSchedule.kindForDay(start + it) }
            assertEquals(3, kinds.count { it == PuzzleKind.CLAIM }, "block=$block CLAIM count")
            assertEquals(2, kinds.count { it == PuzzleKind.LOCATE }, "block=$block LOCATE count")
            assertEquals(2, kinds.count { it == PuzzleKind.WASTED_ASK }, "block=$block WASTED_ASK count")
        }
    }

    @Test
    fun firstThreeWeeksSpanAllKinds() {
        val seen = (0L until 21L).map { PuzzleSchedule.kindForDay(PuzzleSchedule.BASE_EPOCH_DAY + it) }.toSet()
        assertEquals(PuzzleKind.entries.toSet(), seen)
    }
}
