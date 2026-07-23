package com.cards.game.literature.ui.common

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
import com.cards.game.literature.ui.theme.LiteratureTheme

/**
 * Design-time harness for [ConfettiBurst]. A burst is a one-shot animation, so a plain @Preview
 * (which runs with LocalInspectionMode = true) only ever shows it frozen. Hence two modes:
 *
 * - The **static** pane leaves LocalInspectionMode true, so the component freezes itself mid-flight
 *   — a zero-interaction reference for particle look, size and spread.
 * - The **INTERACTIVE** panes force LocalInspectionMode to false, so opening them in Android
 *   Studio's *Interactive Preview* plays the real ~1.4s burst. Tap anywhere to bump the seed and
 *   replay. One pane mirrors the in-game claim burst (26 particles, centre); the other mirrors the
 *   daily-puzzle mini burst (22 particles, launched high).
 */
@Composable
private fun BurstStage(live: Boolean, particleCount: Int, originY: Float) {
    LiteratureTheme(darkTheme = true) {
        // Live panes force inspection off so the one-shot burst actually runs; the static pane
        // passes the ambient value through (true under @Preview) so the burst freezes itself.
        val inspection = if (live) false else LocalInspectionMode.current
        CompositionLocalProvider(LocalInspectionMode provides inspection) {
            var seed by remember { mutableStateOf(1L) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickable { seed += 1 }
            ) {
                // Changing the seed re-keys the burst's internal state and restarts its animation.
                ConfettiBurst(
                    seed = seed,
                    modifier = Modifier.fillMaxSize(),
                    particleCount = particleCount,
                    originY = originY
                )
                Text(
                    if (live) "Tap to replay the burst" else "Static — frozen mid-flight",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                )
            }
        }
    }
}

@Preview(name = "1 · Burst — static (frozen mid-flight)", widthDp = 411, heightDp = 500)
@Composable
private fun BurstStaticPreview() = BurstStage(live = false, particleCount = 26, originY = 0.5f)

@Preview(name = "0 · INTERACTIVE — claim burst (26, centre)", widthDp = 411, heightDp = 500)
@Composable
private fun BurstClaimInteractivePreview() = BurstStage(live = true, particleCount = 26, originY = 0.5f)

@Preview(name = "0 · INTERACTIVE — puzzle mini burst (22, high)", widthDp = 411, heightDp = 500)
@Composable
private fun BurstPuzzleInteractivePreview() = BurstStage(live = true, particleCount = 22, originY = 0.28f)
