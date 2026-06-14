package com.cards.game.literature.ui.stats

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

// Water-blue used to hue-shift the flame at milestone streaks (exact multiples of 50).
private val StreakMilestoneBlue = Color(0xFF1E88E5)

/**
 * Renders a daily-streak value:
 *  - "-" (no emoji) when there is no streak,
 *  - otherwise "N🔥".
 * When N is an exact multiple of 50 the flame is hue-shifted to blue — the Hue blend
 * keeps the emoji's own light/dark structure, giving a natural two-tone blue flame.
 *
 * Note: recoloring a system emoji via BlendMode can render slightly differently across
 * platforms/emoji fonts — worth an eyeball on device; falls back gracefully to the plain
 * flame everywhere it's not a multiple of 50.
 */
@Composable
fun StreakValue(
    streak: Int,
    style: TextStyle,
    color: Color = LocalContentColor.current,
) {
    if (streak <= 0) {
        Text(text = "-", style = style, color = color)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$streak", style = style, color = color)
        val flameModifier = if (streak % 50 == 0) {
            Modifier.drawWithContent {
                drawContent()
                drawRect(color = StreakMilestoneBlue, blendMode = BlendMode.Hue)
            }
        } else {
            Modifier
        }
        Text(text = "🔥", style = style, modifier = flameModifier)
    }
}
