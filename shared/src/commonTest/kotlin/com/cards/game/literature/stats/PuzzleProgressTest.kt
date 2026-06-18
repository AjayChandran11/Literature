package com.cards.game.literature.stats

import kotlin.test.Test
import kotlin.test.assertEquals

class PuzzleProgressTest {

    @Test
    fun consecutiveSolvesBuildStreak() {
        var p = PuzzleProgress()
        p = p.recordAttempt(today = 100, correct = true)
        assertEquals(1, p.currentStreak)
        p = p.recordAttempt(today = 101, correct = true)
        assertEquals(2, p.currentStreak)
        p = p.recordAttempt(today = 102, correct = true)
        assertEquals(3, p.currentStreak)
        assertEquals(3, p.bestStreak)
    }

    @Test
    fun skippedDayResetsStreak() {
        var p = PuzzleProgress()
        p = p.recordAttempt(today = 100, correct = true)
        p = p.recordAttempt(today = 101, correct = true) // streak 2
        p = p.recordAttempt(today = 104, correct = true) // gap -> reset to 1
        assertEquals(1, p.currentStreak)
        assertEquals(2, p.bestStreak)
    }

    @Test
    fun starsByAttemptCount() {
        // 1st-try solve = 3 stars
        assertEquals(3, PuzzleProgress().recordAttempt(1, correct = true).stars)
        // wrong, then solve = 2 stars
        var p = PuzzleProgress().recordAttempt(1, correct = false)
        p = p.recordAttempt(1, correct = true)
        assertEquals(2, p.stars)
        assertEquals(PuzzleStatus.SOLVED, p.status)
    }

    @Test
    fun threeWrongAttemptsFailsTheDay() {
        var p = PuzzleProgress()
        p = p.recordAttempt(1, correct = false)
        assertEquals(PuzzleStatus.IN_PROGRESS, p.status)
        p = p.recordAttempt(1, correct = false)
        p = p.recordAttempt(1, correct = false)
        assertEquals(PuzzleStatus.FAILED, p.status)
        assertEquals(0, p.currentStreak)
    }

    @Test
    fun cannotResolveAfterTerminal() {
        val solved = PuzzleProgress().recordAttempt(1, correct = true)
        // a further attempt the same day is a no-op
        assertEquals(solved, solved.recordAttempt(1, correct = true))
        assertEquals(1, solved.totalSolved)
    }

    @Test
    fun displayedStreakAliveTodayAndYesterdayButNotAfterFail() {
        val solvedToday = PuzzleProgress().recordAttempt(today = 200, correct = true)
        assertEquals(1, solvedToday.displayedStreak(today = 200))   // solved today
        assertEquals(1, solvedToday.displayedStreak(today = 201))   // solved yesterday, today open
        assertEquals(0, solvedToday.displayedStreak(today = 202))   // missed a day

        // solved yesterday then failed today -> broken now
        var p = PuzzleProgress().recordAttempt(today = 200, correct = true)
        p = p.recordAttempt(today = 201, correct = false)
        p = p.recordAttempt(today = 201, correct = false)
        p = p.recordAttempt(today = 201, correct = false) // failed today (201)
        assertEquals(0, p.displayedStreak(today = 201))
    }
}
