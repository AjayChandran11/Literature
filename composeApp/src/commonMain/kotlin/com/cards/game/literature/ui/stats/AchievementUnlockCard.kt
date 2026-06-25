package com.cards.game.literature.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.stats.Achievement
import com.cards.game.literature.ui.theme.GoldAccent
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.achievement_unlocked_banner
import literature.composeapp.generated.resources.achievement_unlocked_more
import org.jetbrains.compose.resources.stringResource

private const val MAX_UNLOCKS_SHOWN = 3

/**
 * Celebratory card listing newly-unlocked achievements (emoji + title + description), capped with a
 * "+N more" tail so a big multi-unlock doesn't push the primary action below the fold. Shared by the
 * game result screen and the Daily Puzzle result.
 */
@Composable
internal fun AchievementUnlockCard(achievements: List<Achievement>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GoldAccent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .border(1.5.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "⭐ " + stringResource(Res.string.achievement_unlocked_banner),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        achievements.take(MAX_UNLOCKS_SHOWN).forEach { achievement ->
            val ui = achievement.ui
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ui.emoji, fontSize = 26.sp)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        stringResource(ui.title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(ui.description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (achievements.size > MAX_UNLOCKS_SHOWN) {
            Text(
                stringResource(Res.string.achievement_unlocked_more, achievements.size - MAX_UNLOCKS_SHOWN),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
