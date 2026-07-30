package com.cards.game.literature.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import com.cards.game.literature.ui.common.ConfettiBurst
import com.cards.game.literature.ui.theme.GoldAccent
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(wash))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSkip
                ),
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
                modifier = Modifier.scale(wordScale.value)
            )
        }
    }
}
