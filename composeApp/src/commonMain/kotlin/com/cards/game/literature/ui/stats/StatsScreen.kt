package com.cards.game.literature.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.currentTimeMillis
import com.cards.game.literature.stats.Achievement
import com.cards.game.literature.stats.MatchRecord
import com.cards.game.literature.stats.Outcome
import com.cards.game.literature.stats.PlayerStats
import com.cards.game.literature.stats.StatsStore
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val stats by StatsStore.stats.collectAsState()
    val history by StatsStore.history.collectAsState()
    val achievements by StatsStore.achievements.collectAsState()
    StatsScreenContent(stats = stats, history = history, achievements = achievements, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsScreenContent(
    stats: PlayerStats,
    history: List<MatchRecord>,
    achievements: Map<String, Long> = emptyMap(),
    onBack: () -> Unit
) {
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.stats_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_stats_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { OverviewTiles(stats) }
            item { StreakRow(stats) }
            item { SkillRow(stats) }
            if (stats.onlineGames > 0) {
                item { OnlineCard(stats) }
            }
            if (stats.gamesByDifficulty.isNotEmpty()) {
                item { VsBotsCard(stats) }
            }
            item {
                AchievementsSection(
                    unlocked = achievements,
                    onSelect = { selectedAchievement = it }
                )
            }
            item {
                Text(
                    stringResource(Res.string.stats_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (history.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.stats_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(history) { record -> HistoryRow(record) }
            }
        }
    }

    selectedAchievement?.let { achievement ->
        AchievementDetailDialog(
            achievement = achievement,
            unlockedAt = achievements[achievement.name],
            onDismiss = { selectedAchievement = null }
        )
    }
}

// ─── Achievements ──────────────────────────────────────────────────────────

@Composable
private fun AchievementsSection(
    unlocked: Map<String, Long>,
    onSelect: (Achievement) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(Res.string.achievements_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(Res.string.achievements_progress, unlocked.size, Achievement.entries.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Achievement.entries.chunked(4).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { achievement ->
                        AchievementBadge(
                            achievement = achievement,
                            isUnlocked = achievement.name in unlocked,
                            onClick = { onSelect(achievement) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Pad the last row so badges keep a uniform width
                    repeat(4 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    achievement: Achievement,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = achievement.ui
    val title = stringResource(ui.title)
    val contentAlpha = if (isUnlocked) 1f else 0.35f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isUnlocked) LightGreen.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (isUnlocked) ui.emoji else "🔒", fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            // Break two-word titles one-word-per-line ("First Victory" ->
            // "First"/"Victory") so they fill both reserved lines instead of
            // wrapping unpredictably by width. Badge-local on purpose — the
            // same strings appear unbroken in the dialog and result card.
            title.replace(' ', '\n'),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal,
            // Reserve two lines for every badge so one-word titles
            // ("Veteran") get the same box height as two-line ones
            minLines = 2,
            maxLines = 2,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun AchievementDetailDialog(
    achievement: Achievement,
    unlockedAt: Long?,
    onDismiss: () -> Unit
) {
    val ui = achievement.ui
    val now = remember { currentTimeMillis() }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(if (unlockedAt != null) ui.emoji else "🔒", fontSize = 36.sp) },
        title = {
            Text(stringResource(ui.title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(ui.description),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (unlockedAt != null) relativeDate(unlockedAt, now)
                    else stringResource(Res.string.achievement_locked),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unlockedAt != null) LightGreen
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.button_ok))
            }
        }
    )
}

@Composable
private fun OverviewTiles(stats: PlayerStats) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            value = "${stats.gamesPlayed}",
            label = stringResource(Res.string.stats_games),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = "${stats.wins}",
            label = stringResource(Res.string.stats_wins),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = stats.winRate.asPercent(),
            label = stringResource(Res.string.stats_win_rate),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StreakRow(stats: PlayerStats) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            value = "🔥 ${stats.currentStreak}",
            label = stringResource(Res.string.stats_current_streak),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = "${stats.bestStreak}",
            label = stringResource(Res.string.stats_best_streak),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SkillRow(stats: PlayerStats) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            value = if (stats.totalAsks == 0) "—" else stats.askSuccessRate.asPercent(),
            label = stringResource(Res.string.stats_ask_success),
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = if (stats.totalClaims == 0) "—" else stats.claimAccuracy.asPercent(),
            label = stringResource(Res.string.stats_claim_accuracy),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OnlineCard(stats: PlayerStats) {
    SectionCard(title = stringResource(Res.string.stats_online_title)) {
        Text(
            stringResource(Res.string.stats_online_record, stats.onlineWins, stats.onlineGames),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun VsBotsCard(stats: PlayerStats) {
    SectionCard(title = stringResource(Res.string.stats_vs_bots_title)) {
        val labels = mapOf(
            "EASY" to stringResource(Res.string.difficulty_easy),
            "MEDIUM" to stringResource(Res.string.difficulty_medium),
            "HARD" to stringResource(Res.string.difficulty_hard)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("EASY", "MEDIUM", "HARD").forEach { key ->
                val games = stats.gamesByDifficulty[key] ?: return@forEach
                val wins = stats.winsByDifficulty[key] ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(labels[key] ?: key, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(Res.string.stats_vs_bots_record, wins, games),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(record: MatchRecord) {
    val now = remember { currentTimeMillis() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutcomeBadge(record.outcome)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${record.myScore} – ${record.opponentScore}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (record.isOnline) {
                    stringResource(Res.string.stats_mode_online, record.playerCount)
                } else {
                    stringResource(Res.string.stats_mode_offline, record.playerCount)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            relativeDate(record.timestamp, now),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OutcomeBadge(outcome: Outcome) {
    val (label, color) = when (outcome) {
        Outcome.WIN -> stringResource(Res.string.stats_outcome_win) to LightGreen
        Outcome.LOSS -> stringResource(Res.string.stats_outcome_loss) to CardRed
        Outcome.DRAW -> stringResource(Res.string.stats_outcome_draw) to MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun relativeDate(timestamp: Long, now: Long): String {
    val days = ((now - timestamp) / 86_400_000L).toInt().coerceAtLeast(0)
    return when (days) {
        0 -> stringResource(Res.string.stats_today)
        1 -> stringResource(Res.string.stats_yesterday)
        else -> stringResource(Res.string.stats_days_ago, days)
    }
}

private fun Float.asPercent(): String = "${(this * 100).toInt()}%"
