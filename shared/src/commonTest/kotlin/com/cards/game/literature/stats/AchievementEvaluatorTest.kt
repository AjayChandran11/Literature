package com.cards.game.literature.stats

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementEvaluatorTest {

    private fun record(
        outcome: Outcome = Outcome.WIN,
        isOnline: Boolean = false,
        asks: Int = 0,
        asksOk: Int = 0,
        claimsOk: Int = 0
    ) = MatchRecord(
        timestamp = 1L,
        isOnline = isOnline,
        playerCount = 4,
        botDifficulty = if (isOnline) null else "MEDIUM",
        myScore = 5,
        opponentScore = 3,
        outcome = outcome,
        myAsks = asks,
        myAsksSuccessful = asksOk,
        myClaims = claimsOk,
        myClaimsCorrect = claimsOk
    )

    private fun satisfied(stats: PlayerStats, rec: MatchRecord = record()) =
        AchievementEvaluator.satisfiedBy(stats, rec)

    @Test
    fun firstWinUnlocksOnFirstWin() {
        val after = PlayerStats().applying(record(Outcome.WIN))
        assertTrue(Achievement.FIRST_WIN in satisfied(after))

        val afterLoss = PlayerStats().applying(record(Outcome.LOSS))
        assertFalse(Achievement.FIRST_WIN in satisfied(afterLoss, record(Outcome.LOSS)))
    }

    @Test
    fun streakAchievementsUnlockAtThresholds() {
        var stats = PlayerStats()
        repeat(2) { stats = stats.applying(record(Outcome.WIN)) }
        assertFalse(Achievement.HAT_TRICK in satisfied(stats))

        stats = stats.applying(record(Outcome.WIN))
        assertTrue(Achievement.HAT_TRICK in satisfied(stats))
        assertFalse(Achievement.ON_FIRE in satisfied(stats))

        repeat(2) { stats = stats.applying(record(Outcome.WIN)) }
        assertTrue(Achievement.ON_FIRE in satisfied(stats))
    }

    @Test
    fun gamesPlayedThresholds() {
        val at24 = PlayerStats(gamesPlayed = 24)
        assertFalse(Achievement.VETERAN in satisfied(at24))

        val at25 = PlayerStats(gamesPlayed = 25)
        assertTrue(Achievement.VETERAN in satisfied(at25))
        assertFalse(Achievement.CENTURION in satisfied(at25))

        val at100 = PlayerStats(gamesPlayed = 100)
        assertTrue(Achievement.CENTURION in satisfied(at100))
    }

    @Test
    fun socialiteUnlocksOnFirstOnlineGameEvenWhenLosing() {
        val rec = record(Outcome.LOSS, isOnline = true)
        val after = PlayerStats().applying(rec)
        assertTrue(Achievement.SOCIALITE in satisfied(after, rec))
    }

    @Test
    fun onlineChampNeedsTenOnlineWins() {
        assertFalse(Achievement.ONLINE_CHAMP in satisfied(PlayerStats(onlineWins = 9)))
        assertTrue(Achievement.ONLINE_CHAMP in satisfied(PlayerStats(onlineWins = 10)))
    }

    @Test
    fun botSlayerNeedsHardWin() {
        val hardLossOnly = PlayerStats(gamesByDifficulty = mapOf("HARD" to 3))
        assertFalse(Achievement.BOT_SLAYER in satisfied(hardLossOnly))

        val hardWin = PlayerStats(winsByDifficulty = mapOf("HARD" to 1))
        assertTrue(Achievement.BOT_SLAYER in satisfied(hardWin))
    }

    @Test
    fun sharpCallerNeedsTenCorrectClaims() {
        assertFalse(Achievement.SHARP_CALLER in satisfied(PlayerStats(correctClaims = 9)))
        assertTrue(Achievement.SHARP_CALLER in satisfied(PlayerStats(correctClaims = 10)))
    }

    @Test
    fun perfectGameRequiresWinAndAllAsksSuccessful() {
        val perfect = record(Outcome.WIN, asks = 6, asksOk = 6)
        assertTrue(Achievement.PERFECT_GAME in satisfied(PlayerStats(), perfect))

        // Not on a loss
        val perfectLoss = record(Outcome.LOSS, asks = 6, asksOk = 6)
        assertFalse(Achievement.PERFECT_GAME in satisfied(PlayerStats(), perfectLoss))

        // Not with a missed ask
        val oneMiss = record(Outcome.WIN, asks = 6, asksOk = 5)
        assertFalse(Achievement.PERFECT_GAME in satisfied(PlayerStats(), oneMiss))

        // Not with fewer than 5 asks (too easy to fluke)
        val tooFew = record(Outcome.WIN, asks = 4, asksOk = 4)
        assertFalse(Achievement.PERFECT_GAME in satisfied(PlayerStats(), tooFew))
    }

    @Test
    fun claimMasterNeedsThreeCorrectClaimsInOneGame() {
        assertFalse(Achievement.CLAIM_MASTER in satisfied(PlayerStats(), record(claimsOk = 2)))
        assertTrue(Achievement.CLAIM_MASTER in satisfied(PlayerStats(), record(claimsOk = 3)))
    }
}
