package com.cards.game.literature.ui.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.viewmodel.PlayerInfo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

/**
 * Design-time harness for the match ceremony ([MatchIntroOverlay] + [MatchStingerOverlay]).
 * Both overlays settle themselves under LocalInspectionMode, so a plain @Preview only ever
 * shows the finished pose. Hence two modes, same idiom as ConfettiBurstPreviews:
 *
 * - **Static** panes leave inspection true — settled-layout references for the team columns,
 *   the "goes first" line, and the stinger word on each outcome wash.
 * - **INTERACTIVE** panes force inspection false; open them in Android Studio's *Interactive
 *   Preview* to watch the real motion. The intro pane runs the full ~2.6s curtain over a mock
 *   board (tapping the curtain mid-run exercises tap-to-skip, exactly like on-device) and
 *   offers Replay when it lifts. The finale pane replays the real end-of-game timeline —
 *   claim banner → board dim (1.8s) → stinger — minus sound and navigation; tapping the
 *   stinger (its onSkip) restarts the sequence. Stinger panes replay on tap the same way.
 */

// Sample rosters built from real PlayerInfo so the previews exercise the same
// emoji-label mapping (matchIntroLabel → BotPersonalities) as the live board.
private fun bot(id: String, name: String) =
    PlayerInfo(id = id, name = name, cardCount = 8, isActive = true, isCurrentTurn = false, isBot = true)

private val teammates6 = listOf(bot("p2", "Alice"), bot("p4", "Charlie"))
private val opponents6 = listOf(bot("p1", "Bob"), bot("p3", "Diana"), bot("p5", "Eve"))
private val teammates8 = teammates6 + bot("p6", "Frank")
private val opponents8 = opponents6 + bot("p7", "Grace")

private fun introData6(firstMoveIsMine: Boolean) = MatchIntroData(
    myTeamLabels = listOf("You") + teammates6.map { it.matchIntroLabel() },
    opponentLabels = opponents6.map { it.matchIntroLabel() },
    firstMoveIsMine = firstMoveIsMine,
    firstMoveName = if (firstMoveIsMine) "You" else "Diana"
)

private val introData8 = MatchIntroData(
    myTeamLabels = listOf("You") + teammates8.map { it.matchIntroLabel() },
    opponentLabels = opponents8.map { it.matchIntroLabel() },
    firstMoveIsMine = false,
    firstMoveName = "Diana"
)

/**
 * Shared pane chrome. Seeds compose-resources' Android context FIRST — both
 * [PreviewContextConfigurationEffect] and stringResource's automatic preview fallback
 * only fire while LocalInspectionMode is still true, so the seeding must happen BEFORE
 * the live panes force inspection off to let one-shot motion play. Without it, live
 * panes crash with MissingResourceException ("Android context is not initialized")
 * the moment an overlay resolves a string.
 */
@Composable
private fun CeremonyStage(live: Boolean, content: @Composable () -> Unit) {
    LiteratureTheme(darkTheme = true) {
        // Runs under the ambient inspection flag (true in preview panes).
        PreviewContextConfigurationEffect()
        val inspection = if (live) false else LocalInspectionMode.current
        CompositionLocalProvider(LocalInspectionMode provides inspection) {
            content()
        }
    }
}

/** A lightweight stand-in for the live board, so the curtain has something to reveal. */
@Composable
private fun FakeBoard() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Team 1  4", color = LightGreen, fontWeight = FontWeight.Bold)
            Text("3  Team 2", color = CardRed, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                // The real board is Material navy/white surfaces — no felt-green table —
                // so the mock must match or the curtain gets judged against a false ground.
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "♠ ♥ ♦ ♣",
                fontSize = 26.sp,
                letterSpacing = 8.sp,
                color = Color.White.copy(alpha = 0.25f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(6) {
                Box(
                    Modifier
                        .size(width = 48.dp, height = 68.dp)
                        .background(Color.White, RoundedCornerShape(6.dp))
                )
            }
        }
    }
}

// ─── Intro ──────────────────────────────────────────────────────────────────

@Composable
private fun IntroStage(live: Boolean, data: MatchIntroData) = CeremonyStage(live) {
    var run by remember { mutableStateOf(0) }
    var curtainUp by remember { mutableStateOf(true) }
    Box(Modifier.fillMaxSize()) {
        FakeBoard()
        if (curtainUp) {
            // Re-keying resets the overlay's beat machine for a fresh run.
            key(run) {
                MatchIntroOverlay(data = data, onDone = { curtainUp = false })
            }
        } else {
            Button(
                onClick = {
                    run += 1
                    curtainUp = true
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("Replay intro")
            }
        }
    }
}

@Preview(name = "0 · INTERACTIVE — intro (full run, tap curtain to skip)", widthDp = 411, heightDp = 800)
@Composable
private fun IntroInteractivePreview() = IntroStage(live = true, data = introData6(firstMoveIsMine = true))

@Preview(name = "1 · Intro — settled · 3v3, you first", widthDp = 411, heightDp = 800)
@Composable
private fun IntroSettled6Preview() = IntroStage(live = false, data = introData6(firstMoveIsMine = true))

@Preview(name = "1 · Intro — settled · 4v4, opponent first", widthDp = 411, heightDp = 800)
@Composable
private fun IntroSettled8Preview() = IntroStage(live = false, data = introData8)

// ─── Stinger ────────────────────────────────────────────────────────────────

@Composable
private fun StingerStage(live: Boolean, isWinner: Boolean, isDraw: Boolean) = CeremonyStage(live) {
    var run by remember { mutableStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        // The overlay's own tap target is onSkip — wired to replay here, so
        // tapping the wash restarts the punch (and the burst, on a win).
        key(run) {
            MatchStingerOverlay(
                visible = true,
                isWinner = isWinner,
                isDraw = isDraw,
                onSkip = { run += 1 }
            )
        }
        Text(
            if (live) "Tap to replay (fires onSkip)" else "Static — settled",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )
    }
}

@Preview(name = "0 · INTERACTIVE — stinger · victory", widthDp = 411, heightDp = 800)
@Composable
private fun StingerVictoryInteractivePreview() = StingerStage(live = true, isWinner = true, isDraw = false)

@Preview(name = "0 · INTERACTIVE — stinger · defeat", widthDp = 411, heightDp = 800)
@Composable
private fun StingerDefeatInteractivePreview() = StingerStage(live = true, isWinner = false, isDraw = false)

@Preview(name = "0 · INTERACTIVE — stinger · draw", widthDp = 411, heightDp = 800)
@Composable
private fun StingerDrawInteractivePreview() = StingerStage(live = true, isWinner = false, isDraw = true)

@Preview(name = "1 · Stinger — settled · victory", widthDp = 411, heightDp = 800)
@Composable
private fun StingerVictorySettledPreview() = StingerStage(live = false, isWinner = true, isDraw = false)

@Preview(name = "1 · Stinger — settled · defeat", widthDp = 411, heightDp = 800)
@Composable
private fun StingerDefeatSettledPreview() = StingerStage(live = false, isWinner = false, isDraw = false)

@Preview(name = "1 · Stinger — settled · draw", widthDp = 411, heightDp = 800)
@Composable
private fun StingerDrawSettledPreview() = StingerStage(live = false, isWinner = false, isDraw = true)

// ─── Full finale sequence ───────────────────────────────────────────────────

/** The real end-of-game timeline (claim → dim @1.8s → stinger), minus sound + navigation. */
@Composable
private fun FinaleSequenceStage() = CeremonyStage(live = true) {
    var run by remember { mutableStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        key(run) {
            var dimmed by remember { mutableStateOf(false) }
            var showStinger by remember { mutableStateOf(false) }
            var celebration by remember {
                mutableStateOf<ClaimCelebrationData?>(
                    ClaimCelebrationData(
                        halfSuit = HalfSuit.HEARTS_HIGH,
                        claimerName = "Alice",
                        byMyTeam = true,
                        correct = true,
                        id = 1L
                    )
                )
            }
            // Mirrors GameBoardContent's finale orchestration and timings.
            LaunchedEffect(Unit) {
                dimmed = true
                delay(1800)
                showStinger = true
            }
            Box(Modifier.fillMaxSize()) {
                FakeBoard()
                val dim by animateFloatAsState(
                    targetValue = if (dimmed) 0.55f else 0f,
                    animationSpec = tween(700),
                    label = "previewFinaleDim"
                )
                if (dim > 0f) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
                }
                ClaimCelebrationOverlay(
                    celebration = celebration,
                    onDone = { celebration = null }
                )
                MatchStingerOverlay(
                    visible = showStinger,
                    isWinner = true,
                    isDraw = false,
                    onSkip = { run += 1 }
                )
            }
        }
        Text(
            "claim banner → dim → stinger · tap the stinger to replay",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.55f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )
    }
}

@Preview(name = "0 · INTERACTIVE — finale (claim → dim → stinger)", widthDp = 411, heightDp = 800)
@Composable
private fun FinaleSequenceInteractivePreview() = FinaleSequenceStage()
