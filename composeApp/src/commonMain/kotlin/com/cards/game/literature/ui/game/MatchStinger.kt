package com.cards.game.literature.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cards.game.literature.ui.common.ConfettiBurst
import com.cards.game.literature.ui.theme.GoldAccent
import kotlinx.coroutines.delay
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.stinger_defeat
import literature.composeapp.generated.resources.stinger_draw
import literature.composeapp.generated.resources.stinger_victory
import org.jetbrains.compose.resources.stringResource

/**
 * The match-finale stinger: a full-screen team-color wash with one Playfair word
 * ("Victory!" / "Defeat" / "It's a Draw") punched in over the dimmed board, shown
 * between the final claim's celebration and the result screen. Purely client-local
 * ceremony — the game is already decided server-side before this renders, so
 * tapping anywhere skips straight to the result screen.
 */
@Composable
fun MatchStingerOverlay(
    visible: Boolean,
    isWinner: Boolean,
    isDraw: Boolean,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(250)),
        modifier = modifier
    ) {
        val inPreview = LocalInspectionMode.current
        // Outcome IS the color here (unlike the neutral intro curtain): green win /
        // red loss / navy draw, matching the app-wide semantics. The win wash starts
        // from DarkGreen so it reads rich, not flat, under the gold word + confetti.
        val wash = when {
            isDraw -> listOf(Color(0xFF26314F), Color(0xFF131320))
            isWinner -> listOf(Color(0xFF1B5E20), Color(0xFF0E2912))
            else -> listOf(Color(0xFF8E2424), Color(0xFF3A0D0D))
        }

        // Same bouncy spring family as the claim banner, scaled up for a
        // full-screen moment.
        val wordScale = remember { Animatable(if (inPreview) 1f else 0.35f) }
        LaunchedEffect(Unit) {
            if (inPreview) return@LaunchedEffect
            wordScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        // Comet flyby: two mirrored comets launch with the word punch, decelerate
        // over the word (trails ruling it above and below), then speed away.
        // Frozen mid-flyby in static previews.
        var boxOrigin by remember { mutableStateOf(Offset.Zero) }
        var wordWindowBounds by remember { mutableStateOf<Rect?>(null) }
        val cometsProgress = remember { Animatable(if (inPreview) 0.5f else 0f) }
        LaunchedEffect(Unit) {
            if (inPreview) return@LaunchedEffect
            delay(100)
            cometsProgress.animateTo(1f, tween(1200, easing = CometFlybyEasing))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(wash))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSkip
                )
                .onGloballyPositioned { boxOrigin = it.boundsInRoot().topLeft },
            contentAlignment = Alignment.Center
        ) {
            if (isWinner) {
                ConfettiBurst(
                    seed = 21L,
                    modifier = Modifier.fillMaxSize(),
                    particleCount = 30,
                    durationMillis = 1600,
                    originY = 0.42f
                )
            }
            StingerComets(
                wordBounds = wordWindowBounds?.translate(-boxOrigin),
                progress = { cometsProgress.value }
            )
            Text(
                text = stringResource(
                    when {
                        isDraw -> Res.string.stinger_draw
                        isWinner -> Res.string.stinger_victory
                        else -> Res.string.stinger_defeat
                    }
                ),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (!isWinner && !isDraw) Color(0xFFFFD7CF) else GoldAccent,
                // Bounds are measured OUTSIDE the punch-scale layer (positioned before
                // scale in the chain), so the comet lines hug the settled word rather
                // than the shrunken first frame — layer changes never re-fire the callback.
                modifier = Modifier
                    .onGloballyPositioned { wordWindowBounds = it.boundsInRoot() }
                    .scale(wordScale.value)
            )
        }
    }
}

/** Fast in, slow over the word, fast out — steep ends, flat middle. */
private val CometFlybyEasing = CubicBezierEasing(0.05f, 0.7f, 0.95f, 0.3f)

/**
 * Two straight gold comets bracketing the stinger word: one enters from the left
 * flying right along the word's top edge, the other from the right flying left
 * along its bottom edge. [CometFlybyEasing]'s slow middle lands while the heads
 * cross the centered word, so the word-width trails momentarily rule it above and
 * below before both lines speed away. Exit overshoot exceeds the trail length so
 * they drain fully off-screen; progress is read in the draw phase so animation
 * frames never recompose the overlay.
 */
@Composable
private fun StingerComets(wordBounds: Rect?, progress: () -> Float, modifier: Modifier = Modifier) {
    if (wordBounds == null) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress()
        if (p <= 0f || p >= 1f) return@Canvas
        val gap = 10.dp.toPx()
        val strokeWidth = 3.5.dp.toPx()
        val margin = 24.dp.toPx()
        val tailLen = (wordBounds.width * 1.15f).coerceAtLeast(120.dp.toPx())
        val travel = size.width + 2 * margin + tailLen

        // Top comet: left -> right along the word's top edge.
        drawComet(
            headX = -margin + travel * p,
            y = wordBounds.top - gap,
            tailLen = tailLen,
            towardRight = true,
            strokeWidth = strokeWidth
        )
        // Bottom comet: right -> left along the bottom edge, mirrored.
        drawComet(
            headX = size.width + margin - travel * p,
            y = wordBounds.bottom + gap,
            tailLen = tailLen,
            towardRight = false,
            strokeWidth = strokeWidth
        )
    }
}

private fun DrawScope.drawComet(
    headX: Float,
    y: Float,
    tailLen: Float,
    towardRight: Boolean,
    strokeWidth: Float
) {
    val tailX = if (towardRight) headX - tailLen else headX + tailLen
    val brush = if (towardRight) {
        Brush.horizontalGradient(
            colors = listOf(GoldAccent.copy(alpha = 0f), GoldAccent.copy(alpha = 0.85f)),
            startX = tailX,
            endX = headX
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(GoldAccent.copy(alpha = 0.85f), GoldAccent.copy(alpha = 0f)),
            startX = headX,
            endX = tailX
        )
    }
    drawLine(
        brush = brush,
        start = Offset(tailX, y),
        end = Offset(headX, y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawCircle(color = GoldAccent, radius = 8.dp.toPx(), center = Offset(headX, y), alpha = 0.22f)
    drawCircle(color = lerp(GoldAccent, Color.White, 0.35f), radius = 3.dp.toPx(), center = Offset(headX, y))
}
