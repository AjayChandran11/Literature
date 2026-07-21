package com.cards.game.literature.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.ui.theme.LightGreen
import kotlinx.coroutines.delay
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.claim_celebration_failed
import literature.composeapp.generated.resources.claim_celebration_ours
import literature.composeapp.generated.resources.claim_celebration_theirs
import org.jetbrains.compose.resources.stringResource
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** One resolved claim, queued for its moment of on-board celebration. */
data class ClaimCelebrationData(
    val halfSuit: HalfSuit,
    val claimerName: String,
    val byMyTeam: Boolean,
    val correct: Boolean,
    /** Unique per event so back-to-back claims each get a fresh animation. */
    val id: Long
)

private val HalfSuit.suitSymbol: String
    get() = when (this) {
        HalfSuit.SPADES_LOW, HalfSuit.SPADES_HIGH -> "♠"
        HalfSuit.HEARTS_LOW, HalfSuit.HEARTS_HIGH -> "♥"
        HalfSuit.DIAMONDS_LOW, HalfSuit.DIAMONDS_HIGH -> "♦"
        HalfSuit.CLUBS_LOW, HalfSuit.CLUBS_HIGH -> "♣"
    }

private val HalfSuit.isRedSuit: Boolean
    get() = this == HalfSuit.HEARTS_LOW || this == HalfSuit.HEARTS_HIGH ||
        this == HalfSuit.DIAMONDS_LOW || this == HalfSuit.DIAMONDS_HIGH

/**
 * The in-game claim moment: a banner springs in over the board announcing the
 * half-suit's fate, with a small confetti burst when it's OUR correct claim.
 * Auto-dismisses after ~2s — claims already pause the flow of play server-side,
 * so this celebrates without blocking anything.
 */
@Composable
fun ClaimCelebrationOverlay(
    celebration: ClaimCelebrationData?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data = celebration ?: return

    // In @Preview panes: skip the entrance/auto-dismiss so static previews show the
    // settled banner instead of catching frame zero (same idiom as the puzzle stars).
    // The interactive preview harness re-enables real motion by overriding this local.
    val inPreview = LocalInspectionMode.current

    LaunchedEffect(data.id) {
        if (inPreview) return@LaunchedEffect
        delay(2000)
        onDone()
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Confetti burst behind the banner — only for our team's correct claims.
        if (data.correct && data.byMyTeam) {
            ClaimConfettiBurst(seed = data.id)
        }

        // Banner springs in from a dip, like the onboarding "CLAIMED!" badge.
        val scale = remember(data.id) { Animatable(if (inPreview) 1f else 0.7f) }
        LaunchedEffect(data.id) {
            if (inPreview) return@LaunchedEffect
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        val accent = when {
            !data.correct -> CardRed
            data.byMyTeam -> LightGreen
            else -> CardRed
        }
        Surface(
            modifier = Modifier.scale(scale.value),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = data.halfSuit.suitSymbol,
                    fontSize = 40.sp,
                    color = if (data.halfSuit.isRedSuit) CardRed else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = data.halfSuit.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = when {
                        !data.correct -> stringResource(Res.string.claim_celebration_failed, data.claimerName)
                        data.byMyTeam -> stringResource(Res.string.claim_celebration_ours)
                        else -> stringResource(Res.string.claim_celebration_theirs)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private class BurstParticle(random: Random) {
    val angle = random.nextFloat() * 2f * 3.14159f
    val speed = 340f + random.nextFloat() * 420f      // px travelled over the burst
    val size = 8f + random.nextFloat() * 10f
    val rotationRate = (random.nextFloat() - 0.5f) * 720f
    val isCircle = random.nextBoolean()
    val color = CONFETTI_COLORS[random.nextInt(CONFETTI_COLORS.size)]

    companion object {
        // Same festive palette as the result-screen confetti.
        val CONFETTI_COLORS = listOf(
            GoldAccent, LightGreen, CardRed,
            Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFF00BCD4)
        )
    }
}

/** A one-shot radial confetti burst from the center — 26 particles, ~1.4s. */
@Composable
private fun ClaimConfettiBurst(seed: Long) {
    val particles = remember(seed) {
        val random = Random(seed)
        List(26) { BurstParticle(random) }
    }
    // Static previews freeze the burst mid-flight so the pane shows the particles.
    val inPreview = LocalInspectionMode.current
    var progress by remember(seed) { mutableStateOf(if (inPreview) 0.35f else 0f) }
    LaunchedEffect(seed) {
        if (inPreview) return@LaunchedEffect
        val anim = Animatable(0f)
        anim.animateTo(1f, tween(durationMillis = 1400, easing = LinearEasing)) {
            progress = value
        }
    }

    if (progress <= 0f || progress >= 1f) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
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
                        size = androidx.compose.ui.geometry.Size(p.size, p.size * 0.65f)
                    )
                }
            }
        }
    }
}
