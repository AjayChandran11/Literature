package com.cards.game.literature.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.currentTimeMillis
import com.cards.game.literature.stats.Achievement
import com.cards.game.literature.stats.MatchRecord
import com.cards.game.literature.stats.Outcome
import com.cards.game.literature.stats.PlayerStats
import com.cards.game.literature.ui.home.HomeStatsCard
import com.cards.game.literature.ui.theme.LiteratureTheme

// ---- Sample data ----

private val typicalStats = PlayerStats(
    gamesPlayed = 27,
    wins = 16,
    losses = 9,
    draws = 2,
    onlineGames = 8,
    onlineWins = 5,
    currentStreak = 3,
    bestStreak = 6,
    totalAsks = 412,
    successfulAsks = 233,
    totalClaims = 31,
    correctClaims = 24,
    gamesByDifficulty = mapOf("EASY" to 5, "MEDIUM" to 10, "HARD" to 4),
    winsByDifficulty = mapOf("EASY" to 5, "MEDIUM" to 6, "HARD" to 1)
)

/** Stress case: every number at 3 digits / 100%. */
private val veteranStats = PlayerStats(
    gamesPlayed = 999,
    wins = 999,
    losses = 0,
    draws = 0,
    onlineGames = 458,
    onlineWins = 458,
    currentStreak = 142,
    bestStreak = 142,
    totalAsks = 18432,
    successfulAsks = 18432,
    totalClaims = 1204,
    correctClaims = 1204,
    gamesByDifficulty = mapOf("EASY" to 333, "MEDIUM" to 108, "HARD" to 100),
    winsByDifficulty = mapOf("EASY" to 333, "MEDIUM" to 108, "HARD" to 100)
)

private val someAchievements: Map<String, Long> = mapOf(
    Achievement.FIRST_WIN.name to 1L,
    Achievement.HAT_TRICK.name to 2L,
    Achievement.SOCIALITE.name to 3L,
    Achievement.SHARP_CALLER.name to 4L
)

private val allAchievements: Map<String, Long> =
    Achievement.entries.associate { it.name to 1L }

private fun sampleHistory(count: Int): List<MatchRecord> {
    val now = currentTimeMillis()
    val outcomes = listOf(Outcome.WIN, Outcome.WIN, Outcome.LOSS, Outcome.DRAW, Outcome.WIN)
    return (0 until count).map { i ->
        val outcome = outcomes[i % outcomes.size]
        MatchRecord(
            timestamp = now - i * 86_400_000L / 2, // every ~12h back
            isOnline = i % 3 == 0,
            playerCount = listOf(4, 6, 8)[i % 3],
            botDifficulty = if (i % 3 == 0) null else listOf("EASY", "MEDIUM", "HARD")[i % 3],
            myScore = if (outcome == Outcome.WIN) 5 + i % 3 else 3,
            opponentScore = if (outcome == Outcome.WIN) 3 else if (outcome == Outcome.DRAW) 3 else 5,
            outcome = outcome,
            myAsks = 12 + i,
            myAsksSuccessful = 7,
            myClaims = 2,
            myClaimsCorrect = 1
        )
    }
}

// ---- Streak value (incl. blue flame at multiples of 50) ----

@Preview(showBackground = true, name = "Streak values — 0/1/11/49/50/100")
@Composable
private fun StreakValuePreview() {
    LiteratureTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(0, 1, 11, 49, 50, 100).forEach { n ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("n=$n", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StreakValue(
                            streak = n,
                            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// ---- Home stats strip ----

@Preview(showBackground = true, name = "Home strip — typical")
@Composable
private fun HomeStatsCardPreview() {
    LiteratureTheme {
        Surface {
            HomeStatsCard(stats = typicalStats)
        }
    }
}

@Preview(showBackground = true, name = "Home strip — 3-digit stress")
@Composable
private fun HomeStatsCardStressPreview() {
    LiteratureTheme {
        Surface(modifier = Modifier.padding(8.dp)) {
            HomeStatsCard(stats = veteranStats)
        }
    }
}

@Preview(showBackground = true, name = "Home strip — narrow device", widthDp = 320)
@Composable
private fun HomeStatsCardNarrowPreview() {
    LiteratureTheme {
        Surface {
            HomeStatsCard(stats = veteranStats)
        }
    }
}

// ---- Stats screen ----

@Preview(showBackground = true, name = "Stats — typical", heightDp = 1700)
@Composable
private fun StatsScreenPreview() {
    LiteratureTheme {
        StatsScreenContent(
            stats = typicalStats,
            history = sampleHistory(8),
            achievements = someAchievements,
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Stats — veteran, full history", heightDp = 2300)
@Composable
private fun StatsScreenVeteranPreview() {
    LiteratureTheme {
        StatsScreenContent(
            stats = veteranStats,
            history = sampleHistory(50),
            achievements = allAchievements,
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Stats — brand new player")
@Composable
private fun StatsScreenEmptyPreview() {
    LiteratureTheme {
        StatsScreenContent(stats = PlayerStats(), history = emptyList(), onBack = {})
    }
}

@Preview(showBackground = true, name = "Stats — dark mode", heightDp = 1700)
@Composable
private fun StatsScreenDarkPreview() {
    LiteratureTheme(darkTheme = true) {
        StatsScreenContent(
            stats = typicalStats,
            history = sampleHistory(8),
            achievements = someAchievements,
            onBack = {}
        )
    }
}
