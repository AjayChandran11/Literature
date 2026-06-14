package com.cards.game.literature.stats

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerStatsTest {

    private fun record(
        outcome: Outcome,
        isOnline: Boolean = false,
        difficulty: String? = "MEDIUM",
        asks: Int = 0,
        asksOk: Int = 0,
        claims: Int = 0,
        claimsOk: Int = 0
    ) = MatchRecord(
        timestamp = 1L,
        isOnline = isOnline,
        playerCount = 4,
        botDifficulty = difficulty,
        myScore = if (outcome == Outcome.WIN) 5 else 3,
        opponentScore = if (outcome == Outcome.WIN) 3 else 5,
        outcome = outcome,
        myAsks = asks,
        myAsksSuccessful = asksOk,
        myClaims = claims,
        myClaimsCorrect = claimsOk
    )

    @Test
    fun winIncrementsCountersAndStreak() {
        val stats = PlayerStats().applying(record(Outcome.WIN))
        assertEquals(1, stats.gamesPlayed)
        assertEquals(1, stats.wins)
        assertEquals(0, stats.losses)
        assertEquals(1, stats.currentStreak)
        assertEquals(1, stats.bestStreak)
    }

    @Test
    fun lossResetsStreakButKeepsBest() {
        var stats = PlayerStats()
        repeat(3) { stats = stats.applying(record(Outcome.WIN)) }
        stats = stats.applying(record(Outcome.LOSS))

        assertEquals(0, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
        assertEquals(1, stats.losses)
        assertEquals(4, stats.gamesPlayed)
    }

    @Test
    fun drawResetsStreakAndCountsAsDraw() {
        var stats = PlayerStats().applying(record(Outcome.WIN))
        stats = stats.applying(record(Outcome.DRAW))

        assertEquals(0, stats.currentStreak)
        assertEquals(1, stats.draws)
        assertEquals(1, stats.wins)
    }

    @Test
    fun streakRebuildsAfterReset() {
        var stats = PlayerStats()
        repeat(2) { stats = stats.applying(record(Outcome.WIN)) }
        stats = stats.applying(record(Outcome.LOSS))
        repeat(4) { stats = stats.applying(record(Outcome.WIN)) }

        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.bestStreak)
    }

    @Test
    fun onlineCountersOnlyTouchedByOnlineGames() {
        var stats = PlayerStats().applying(record(Outcome.WIN, isOnline = false))
        assertEquals(0, stats.onlineGames)

        stats = stats.applying(record(Outcome.WIN, isOnline = true, difficulty = null))
        stats = stats.applying(record(Outcome.LOSS, isOnline = true, difficulty = null))

        assertEquals(2, stats.onlineGames)
        assertEquals(1, stats.onlineWins)
    }

    @Test
    fun askAndClaimTotalsAccumulate() {
        var stats = PlayerStats().applying(record(Outcome.WIN, asks = 10, asksOk = 6, claims = 2, claimsOk = 2))
        stats = stats.applying(record(Outcome.LOSS, asks = 5, asksOk = 1, claims = 1, claimsOk = 0))

        assertEquals(15, stats.totalAsks)
        assertEquals(7, stats.successfulAsks)
        assertEquals(3, stats.totalClaims)
        assertEquals(2, stats.correctClaims)
    }

    @Test
    fun difficultyMapsTrackOfflineGamesOnly() {
        var stats = PlayerStats()
        stats = stats.applying(record(Outcome.WIN, difficulty = "HARD"))
        stats = stats.applying(record(Outcome.LOSS, difficulty = "HARD"))
        stats = stats.applying(record(Outcome.WIN, isOnline = true, difficulty = null))

        assertEquals(2, stats.gamesByDifficulty["HARD"])
        assertEquals(1, stats.winsByDifficulty["HARD"])
        assertEquals(null, stats.gamesByDifficulty["MEDIUM"])
    }

    @Test
    fun dailyStreakStateMachine() {
        assertEquals(1, nextDailyStreak(lastDay = 0, today = 100, current = 0))   // first game ever
        assertEquals(5, nextDailyStreak(lastDay = 100, today = 100, current = 5)) // same day — unchanged
        assertEquals(6, nextDailyStreak(lastDay = 100, today = 101, current = 5)) // next day — +1
        assertEquals(1, nextDailyStreak(lastDay = 100, today = 103, current = 5)) // gap >= 2 — reset
        assertEquals(5, nextDailyStreak(lastDay = 100, today = 99, current = 5))  // out-of-order — unchanged
    }

    @Test
    fun displayedDailyStreakBreaksAfterMissedDay() {
        val s = PlayerStats(lastPlayedEpochDay = 100, currentDailyStreak = 7)
        assertEquals(7, s.displayedDailyStreak(today = 100)) // played today
        assertEquals(7, s.displayedDailyStreak(today = 101)) // played yesterday — still alive
        assertEquals(0, s.displayedDailyStreak(today = 102)) // missed a full day — broken
        assertEquals(0, PlayerStats().displayedDailyStreak(today = 100)) // never played
    }

    @Test
    fun applyingRecordSeedsDailyStreak() {
        val stats = PlayerStats().applying(record(Outcome.WIN)) // timestamp = 1L
        assertEquals(1, stats.currentDailyStreak)
        assertEquals(1, stats.bestDailyStreak)
        assertEquals(localEpochDay(1L), stats.lastPlayedEpochDay)
    }

    @Test
    fun bestDailyStreakSurvivesAReset() {
        // On a 20-day streak as of day0...
        val day0 = localEpochDay(0L)
        val stats = PlayerStats(lastPlayedEpochDay = day0, currentDailyStreak = 20, bestDailyStreak = 20)
        // ...then return after a long gap: current resets to 1, best stays 20.
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val result = stats.applying(record(Outcome.WIN).copy(timestamp = thirtyDaysMs))
        assertEquals(1, result.currentDailyStreak)
        assertEquals(20, result.bestDailyStreak)
    }

    @Test
    fun ratesComputeSafelyFromZero() {
        val empty = PlayerStats()
        assertEquals(0f, empty.winRate)
        assertEquals(0f, empty.askSuccessRate)
        assertEquals(0f, empty.claimAccuracy)

        val stats = empty.applying(record(Outcome.WIN, asks = 4, asksOk = 3, claims = 2, claimsOk = 1))
        assertEquals(1f, stats.winRate)
        assertEquals(0.75f, stats.askSuccessRate)
        assertEquals(0.5f, stats.claimAccuracy)
    }
}
