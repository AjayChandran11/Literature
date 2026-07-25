package com.cards.game.literature.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalInspectionMode
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.ui.theme.LightGreen
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The festive palette shared by every in-app celebration burst. */
val ConfettiColors = listOf(
    GoldAccent, LightGreen, CardRed,
    Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFF00BCD4)
)

private class ConfettiParticle(random: Random, colors: List<Color>) {
    val angle = random.nextFloat() * 2f * 3.14159f
    val speed = 340f + random.nextFloat() * 420f      // px travelled over the burst
    val size = 8f + random.nextFloat() * 10f
    val rotationRate = (random.nextFloat() - 0.5f) * 720f
    val isCircle = random.nextBoolean()
    val color = colors[random.nextInt(colors.size)]
}

/**
 * A one-shot radial confetti burst — the shared celebration primitive behind both the in-game
 * claim moment and the daily-puzzle solve.
 *
 * The caller sizes it (e.g. `Modifier.fillMaxSize()` for a full-screen overlay, or
 * `Modifier.matchParentSize()` to sit over a card). Particles are drawn *beyond* those bounds,
 * so place it as a non-clipping overlay — anything clipping the burst crops it. [originX]/[originY]
 * are the fractional launch point (0..1) within those bounds. [seed] makes the pattern
 * deterministic and re-runs the animation whenever it changes.
 */
@Composable
fun ConfettiBurst(
    seed: Long,
    modifier: Modifier = Modifier,
    particleCount: Int = 26,
    durationMillis: Int = 1400,
    originX: Float = 0.5f,
    originY: Float = 0.5f,
    colors: List<Color> = ConfettiColors
) {
    val particles = remember(seed, particleCount) {
        val random = Random(seed)
        List(particleCount) { ConfettiParticle(random, colors) }
    }
    // Static previews freeze the burst mid-flight so the pane shows the particles.
    val inPreview = LocalInspectionMode.current
    var progress by remember(seed) { mutableStateOf(if (inPreview) 0.35f else 0f) }
    LaunchedEffect(seed) {
        if (inPreview) return@LaunchedEffect
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(durationMillis = durationMillis, easing = LinearEasing)) {
            progress = value
        }
    }

    if (progress <= 0f || progress >= 1f) return
    Canvas(modifier = modifier) {
        val center = Offset(size.width * originX, size.height * originY)
        // Ease the burst: fast exit, slowing as it spreads.
        val spread = 1f - (1f - progress) * (1f - progress)
        val alpha = if (progress < 0.6f) 1f else 1f - (progress - 0.6f) / 0.4f
        particles.forEach { p ->
            val x = center.x + cos(p.angle) * p.speed * spread
            // Slight gravity pull as the burst decays.
            val y = center.y + sin(p.angle) * p.speed * spread + 260f * progress * progress
            rotate(degrees = p.rotationRate * progress, pivot = Offset(x, y)) {
                if (p.isCircle) {
                    drawCircle(p.color.copy(alpha = alpha), radius = p.size / 2f, center = Offset(x, y))
                } else {
                    drawRect(
                        p.color.copy(alpha = alpha),
                        topLeft = Offset(x - p.size / 2f, y - p.size / 2f),
                        size = Size(p.size, p.size * 0.65f)
                    )
                }
            }
        }
    }
}
