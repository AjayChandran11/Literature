package com.cards.game.literature.stats

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PuzzleAchievementEvaluatorTest {

    /** A post-solve progress with just the fields the evaluator reads. */
    private fun progress(totalSolved: Int = 0, currentStreak: Int = 0, stars: Int = 0) =
        PuzzleProgress(currentStreak = currentStreak, totalSolved = totalSolved, stars = stars)

    @Test
    fun nothingUnlockedBeforeAnySolve() {
        assertTrue(PuzzleAchievementEvaluator.satisfiedBy(PuzzleProgress()).isEmpty())
    }

    @Test
    fun firstSolveUnlocksRookieOnly() {
        val r = PuzzleAchievementEvaluator.satisfiedBy(progress(totalSolved = 1, currentStreak = 1, stars = 2))
        assertTrue(Achievement.PUZZLE_ROOKIE in r)
        assertFalse(Achievement.PUZZLE_STREAK_7 in r)
        assertFalse(Achievement.PUZZLE_FIFTY in r)
        assertFalse(Achievement.PUZZLE_FLAWLESS in r) // 2 stars, not a first-try solve
    }

    @Test
    fun flawlessNeedsAllThreeStars() {
        assertFalse(Achievement.PUZZLE_FLAWLESS in PuzzleAchievementEvaluator.satisfiedBy(progress(totalSolved = 1, stars = 2)))
        assertTrue(Achievement.PUZZLE_FLAWLESS in PuzzleAchievementEvaluator.satisfiedBy(progress(totalSolved = 1, stars = 3)))
    }

    @Test
    fun streakThresholds() {
        assertFalse(Achievement.PUZZLE_STREAK_7 in PuzzleAchievementEvaluator.satisfiedBy(progress(totalSolved = 6, currentStreak = 6)))
        val seven = PuzzleAchievementEvaluator.satisfiedBy(progress(totalSolved = 7, currentStreak = 7))
        assertTrue(Achievement.PUZZLE_STREAK_7 in seven)
        assertFalse(Achievement.PUZZLE_STREAK_30 in seven)
        assertTrue(Achievement.PUZZLE_STREAK_30 in PuzzleAchievementEvaluator.satisfiedBy(progress(totalSolved = 30, currentStreak = 30)))
    }

    @Test
    fun fiftyThreshold() {
        assertFalse(Achievement.PUZZLE_FIFTY in PuzzleAchievementEvaluator.satisfiedBy(progress(totalSolved = 49)))
        assertTrue(Achievement.PUZZLE_FIFTY in PuzzleAchievementEvaluator.satisfiedBy(progress(totalSolved = 50)))
    }

    @Test
    fun evaluatorReturnsOnlyPuzzleAchievements() {
        // A maxed-out puzzle progress must never unlock a gameplay achievement.
        val all = PuzzleAchievementEvaluator.satisfiedBy(progress(totalSolved = 100, currentStreak = 100, stars = 3))
        assertTrue(all.all { it.name.startsWith("PUZZLE_") })
    }
}
