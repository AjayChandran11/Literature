package com.cards.game.literature.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.ui.theme.LiteratureTheme

/**
 * Design-time harness for the in-game claim celebration. On a real device a claim
 * only fires when someone actually collects (or fumbles) a half-suit, so to iterate
 * on the banner we drive [ClaimCelebrationOverlay] directly with hand-built
 * [ClaimCelebrationData] — mirror of the [DailyPuzzlePreviews] approach.
 *
 * - Static previews freeze the entrance (banner settled, confetti mid-burst) via
 *   the overlay's LocalInspectionMode handling.
 * - The INTERACTIVE preview overrides LocalInspectionMode to false, so opening it
 *   in Android Studio's *Interactive Preview* plays the real spring + confetti +
 *   auto-dismiss; tap anywhere to replay, cycling through all three variants.
 */

private val oursCorrect = ClaimCelebrationData(
    halfSuit = HalfSuit.HEARTS_LOW,
    claimerName = "You",
    byMyTeam = true,
    correct = true,
    id = 1L
)

private val theirsCorrect = ClaimCelebrationData(
    halfSuit = HalfSuit.SPADES_HIGH,
    claimerName = "Alice",
    byMyTeam = false,
    correct = true,
    id = 2L
)

private val failed = ClaimCelebrationData(
    halfSuit = HalfSuit.DIAMONDS_HIGH,
    claimerName = "Bob",
    byMyTeam = false,
    correct = false,
    id = 3L
)

@Composable
private fun PreviewSurface(darkTheme: Boolean, data: ClaimCelebrationData) {
    LiteratureTheme(darkTheme = darkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ClaimCelebrationOverlay(celebration = data, onDone = {})
        }
    }
}

// ── Static panes: every variant, both themes ─────────────────────────────────

@Preview(name = "1 · Ours correct + confetti · dark", widthDp = 411, heightDp = 500)
@Composable
private fun OursCorrectDarkPreview() = PreviewSurface(darkTheme = true, data = oursCorrect)

@Preview(name = "2 · Ours correct + confetti · light", widthDp = 411, heightDp = 500)
@Composable
private fun OursCorrectLightPreview() = PreviewSurface(darkTheme = false, data = oursCorrect)

@Preview(name = "3 · Opponents correct · dark", widthDp = 411, heightDp = 500)
@Composable
private fun TheirsCorrectDarkPreview() = PreviewSurface(darkTheme = true, data = theirsCorrect)

@Preview(name = "4 · Opponents correct · light", widthDp = 411, heightDp = 500)
@Composable
private fun TheirsCorrectLightPreview() = PreviewSurface(darkTheme = false, data = theirsCorrect)

@Preview(name = "5 · Failed claim · dark", widthDp = 411, heightDp = 500)
@Composable
private fun FailedDarkPreview() = PreviewSurface(darkTheme = true, data = failed)

@Preview(name = "6 · Failed claim · light", widthDp = 411, heightDp = 500)
@Composable
private fun FailedLightPreview() = PreviewSurface(darkTheme = false, data = failed)

// ── Interactive: real motion, tap to replay ──────────────────────────────────

@Preview(name = "INTERACTIVE · tap to replay real motion", widthDp = 411, heightDp = 600)
@Composable
private fun ClaimCelebrationInteractivePreview() {
    val variants = listOf(oursCorrect, theirsCorrect, failed)
    var counter by remember { mutableStateOf(0) }
    var data by remember { mutableStateOf<ClaimCelebrationData?>(null) }

    LiteratureTheme(darkTheme = true) {
        // Overlay checks LocalInspectionMode to freeze itself in static panes;
        // forcing it false here restores the runtime spring/confetti/auto-dismiss.
        CompositionLocalProvider(LocalInspectionMode provides false) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable {
                        counter += 1
                        data = variants[counter % variants.size].copy(id = counter.toLong())
                    }
            ) {
                Text(
                    text = if (data == null) "Tap to fire a claim" else "Tap to replay (next variant)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                )
                ClaimCelebrationOverlay(
                    celebration = data,
                    onDone = { data = null }
                )
            }
        }
    }
}
