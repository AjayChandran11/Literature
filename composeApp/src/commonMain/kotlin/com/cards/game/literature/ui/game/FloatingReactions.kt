package com.cards.game.literature.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.random.Random

data class DisplayReaction(
    val id: Long,
    val senderId: String,
    val senderName: String,
    val emoji: String
)

@Composable
fun FloatingReactions(
    activeReactions: List<DisplayReaction>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }

        activeReactions.forEach { reaction ->
            key(reaction.id) {
                FloatingEmoji(
                    reaction = reaction,
                    maxHeightPx = maxHeightPx,
                    maxWidthPx = maxWidthPx
                )
            }
        }
    }
}

@Composable
private fun FloatingEmoji(
    reaction: DisplayReaction,
    maxHeightPx: Float,
    maxWidthPx: Float
) {
    val progress = remember { Animatable(0f) }
    // Random horizontal offset for visual variety
    val xOffsetPx = remember { Random.nextFloat() * 80f - 40f }
    // Start X position: right side (~75-90% of width)
    val baseXFraction = remember { 0.75f + Random.nextFloat() * 0.15f }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3000)
        )
    }

    val p = progress.value
    // Y: starts at 85% of height, rises to 15%
    val yPx = maxHeightPx * (0.85f - p * 0.70f)
    // X: base position + slight drift with progress
    val xPx = maxWidthPx * baseXFraction + xOffsetPx * p
    // Alpha: fully visible until 60%, then fade out
    val alpha = if (p < 0.6f) 1f else 1f - ((p - 0.6f) / 0.4f)
    // Scale: starts at 1.0, shrinks to 0.6
    val scale = 1f - p * 0.4f

    Box(
        modifier = Modifier
            .offset { IntOffset(xPx.roundToInt(), yPx.roundToInt()) }
            .alpha(alpha)
            .scale(scale)
    ) {
        Text(
            text = reaction.emoji,
            fontSize = 28.sp
        )
    }
}
