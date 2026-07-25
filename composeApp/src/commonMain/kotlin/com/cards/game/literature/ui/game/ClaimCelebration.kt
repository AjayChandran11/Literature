package com.cards.game.literature.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.ui.common.ConfettiBurst
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import kotlinx.coroutines.delay
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.claim_celebration_failed
import literature.composeapp.generated.resources.claim_celebration_ours
import literature.composeapp.generated.resources.claim_celebration_theirs
import org.jetbrains.compose.resources.stringResource

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
            ConfettiBurst(seed = data.id, modifier = Modifier.fillMaxSize())
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
