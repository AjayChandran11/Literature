package com.cards.game.literature.ui.result

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.cards.game.literature.audio.SoundEvent
import com.cards.game.literature.audio.SoundPlayer
import com.cards.game.literature.preferences.GamePrefs
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.model.Suit
import com.cards.game.literature.ui.game.GameLogEntry
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.viewmodel.ResultUiState
import com.cards.game.literature.viewmodel.ResultViewModel
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.button_home
import literature.composeapp.generated.resources.button_play_again
import literature.composeapp.generated.resources.label_opponents
import literature.composeapp.generated.resources.label_your_team
import literature.composeapp.generated.resources.result_breakdown_title
import literature.composeapp.generated.resources.result_draw
import literature.composeapp.generated.resources.result_hide_log
import literature.composeapp.generated.resources.result_lose
import literature.composeapp.generated.resources.result_show_log
import literature.composeapp.generated.resources.result_unclaimed
import literature.composeapp.generated.resources.result_win
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random

// ─── Confetti particle model ───────────────────────────────────────────────

private data class ConfettiParticle(
    val x: Float,         // initial x (0..1 of screen width)
    val speedY: Float,    // fall speed multiplier
    val speedX: Float,    // horizontal drift
    val rotation: Float,  // initial rotation degrees
    val rotationSpeed: Float,
    val size: Float,      // rect half-size in px
    val color: Color,
    val shape: Int,       // 0 = rect, 1 = circle
)

private val confettiColors = listOf(
    Color(0xFFFFD700), // gold
    Color(0xFF4CAF50), // green
    Color(0xFFE91E63), // pink
    Color(0xFF2196F3), // blue
    Color(0xFFFF5722), // orange
    Color(0xFF9C27B0), // purple
    Color(0xFF00BCD4), // cyan
    Color(0xFFFFEB3B), // yellow
)

private fun generateParticles(count: Int): List<ConfettiParticle> {
    val rng = Random(seed = 42)
    return List(count) {
        ConfettiParticle(
            x = rng.nextFloat(),
            speedY = 0.4f + rng.nextFloat() * 0.6f,
            speedX = (rng.nextFloat() - 0.5f) * 0.3f,
            rotation = rng.nextFloat() * 360f,
            rotationSpeed = (rng.nextFloat() - 0.5f) * 4f,
            size = 6f + rng.nextFloat() * 8f,
            color = confettiColors[it % confettiColors.size],
            shape = rng.nextInt(2),
        )
    }
}

// ─── Confetti overlay ─────────────────────────────────────────────────────

@Composable
private fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val particles = remember { generateParticles(80) }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
        )
    }

    val p = progress.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        particles.forEach { particle ->
            val y = (particle.speedY * p * (h + particle.size * 2)) - particle.size
            val x = (particle.x * w) + (particle.speedX * p * w)
            val rot = particle.rotation + particle.rotationSpeed * p * 360f

            if (y < h + particle.size * 2) {
                val alpha = if (p > 0.75f) 1f - ((p - 0.75f) / 0.25f) else 1f

                withTransform({
                    translate(x, y)
                    rotate(degrees = rot, pivot = Offset.Zero)
                }) {
                    if (particle.shape == 0) {
                        drawRect(
                            color = particle.color.copy(alpha = alpha),
                            topLeft = Offset(-particle.size, -particle.size / 2),
                            size = Size(particle.size * 2, particle.size),
                        )
                    } else {
                        drawCircle(
                            color = particle.color.copy(alpha = alpha),
                            radius = particle.size / 2,
                            center = Offset.Zero,
                        )
                    }
                }
            }
        }
    }
}

// ─── Breakdown row with stagger animation ─────────────────────────────────

@Composable
private fun BreakdownRow(
    status: HalfSuitStatus,
    index: Int,
    visible: Boolean,
    myTeamName: String,
    opponentTeamName: String,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = if (visible) index * 60 else 0,
            easing = EaseOut
        ),
        label = "rowAlpha$index"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 20f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = if (visible) index * 60 else 0,
            easing = EaseOutBack
        ),
        label = "rowOffset$index"
    )
    val claimedBy = when (status.claimedByTeamId) {
        "team_1" -> myTeamName
        "team_2" -> opponentTeamName
        else -> stringResource(Res.string.result_unclaimed)
    }
    val claimColor = when (status.claimedByTeamId) {
        "team_1" -> LightGreen
        "team_2" -> CardRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY
            },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            status.halfSuit.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            claimedBy,
            style = MaterialTheme.typography.bodyMedium,
            color = claimColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Screens ──────────────────────────────────────────────────────────────

@Composable
fun ResultScreen(
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: ResultViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLog by remember { mutableStateOf(false) }

    ResultScreenContent(
        uiState = uiState,
        showLog = showLog,
        onToggleLog = { showLog = !showLog },
        onPlayAgain = onPlayAgain,
        onGoHome = onGoHome
    )
}

@Composable
fun ResultScreenContent(
    uiState: ResultUiState,
    showLog: Boolean,
    onToggleLog: () -> Unit,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    // ── Sound + haptics ──────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        if (uiState.isWinner) {
            SoundPlayer.play(SoundEvent.GAME_WIN)
            if (GamePrefs.isHapticsEnabled()) {
                repeat(3) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(80)
                }
            }
        } else {
            SoundPlayer.play(SoundEvent.GAME_LOSE)
            if (GamePrefs.isHapticsEnabled()) {
                repeat(2) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    delay(80)
                }
            }
        }
    }

    // ── Banner entrance: visible after short delay ────────────────────────
    var bannerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        bannerVisible = true
    }

    // ── Score count-up ────────────────────────────────────────────────────
    val animatedMyScore by animateIntAsState(
        targetValue = uiState.myTeamScore,
        animationSpec = tween(durationMillis = 1200, delayMillis = 400, easing = EaseOut),
        label = "myScore"
    )
    val animatedOpponentScore by animateIntAsState(
        targetValue = uiState.opponentTeamScore,
        animationSpec = tween(durationMillis = 1200, delayMillis = 400, easing = EaseOut),
        label = "opponentScore"
    )

    // ── Winner banner pulse (win only) ────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "bannerPulse")
    val bannerScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.isWinner) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bannerScale"
    )

    // ── Breakdown stagger: reveal rows progressively ──────────────────────
    var breakdownVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(800)
        breakdownVisible = true
    }

    val myTeamDisplayName = uiState.myTeamName.ifEmpty { stringResource(Res.string.label_your_team) }
    val opponentTeamDisplayName = uiState.opponentTeamName.ifEmpty { stringResource(Res.string.label_opponents) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Winner banner ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = bannerVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = tween(500, easing = EaseOutBack)
                ) + scaleIn(
                    initialScale = 0.6f,
                    animationSpec = tween(500, easing = EaseOutBack)
                ) + fadeIn(animationSpec = tween(300))
            ) {
                Text(
                    text = when {
                        uiState.isDraw -> stringResource(Res.string.result_draw)
                        uiState.isWinner -> stringResource(Res.string.result_win)
                        else -> stringResource(Res.string.result_lose)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    color = when {
                        uiState.isDraw -> MaterialTheme.colorScheme.secondary
                        uiState.isWinner -> LightGreen
                        else -> CardRed
                    },
                    modifier = Modifier.scale(if (uiState.isWinner) bannerScale else 1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Score ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        myTeamDisplayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$animatedMyScore",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = LightGreen
                    )
                }
                Text(
                    "-",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        opponentTeamDisplayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$animatedOpponentScore",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = CardRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Breakdown ─────────────────────────────────────────────────
            Text(
                stringResource(Res.string.result_breakdown_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                uiState.halfSuitBreakdown.forEachIndexed { index, status ->
                    BreakdownRow(
                        status = status,
                        index = index,
                        visible = breakdownVisible,
                        myTeamName = myTeamDisplayName,
                        opponentTeamName = opponentTeamDisplayName,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Game Log ──────────────────────────────────────────────────
            OutlinedButton(
                onClick = onToggleLog,
                modifier = Modifier.fillMaxWidth(0.7f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (showLog) stringResource(Res.string.result_hide_log) else stringResource(Res.string.result_show_log))
            }

            if (showLog) {
                Spacer(modifier = Modifier.height(8.dp))
                val displayEvents = uiState.gameLog.filterNot {
                    it is GameEvent.TurnChanged || it is GameEvent.GameStarted
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    items(displayEvents) { event ->
                        GameLogEntry(event = event, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(Res.string.button_play_again), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(Res.string.button_home))
            }
        }

        // ── Confetti (win only, on top of everything) ─────────────────────
        if (uiState.isWinner) {
            ConfettiOverlay()
        }
    }
}

// ─── Preview data ──────────────────────────────────────────────────────────

private val previewBreakdown = listOf(
    HalfSuitStatus(HalfSuit.SPADES_LOW, claimedByTeamId = "team_1"),
    HalfSuitStatus(HalfSuit.SPADES_HIGH, claimedByTeamId = "team_1"),
    HalfSuitStatus(HalfSuit.HEARTS_LOW, claimedByTeamId = "team_2"),
    HalfSuitStatus(HalfSuit.HEARTS_HIGH, claimedByTeamId = "team_2"),
    HalfSuitStatus(HalfSuit.DIAMONDS_LOW, claimedByTeamId = "team_1"),
    HalfSuitStatus(HalfSuit.DIAMONDS_HIGH, claimedByTeamId = "team_2"),
    HalfSuitStatus(HalfSuit.CLUBS_LOW, claimedByTeamId = "team_1"),
    HalfSuitStatus(HalfSuit.CLUBS_HIGH, claimedByTeamId = null),
)

private val previewGameLog = buildList {
    repeat(30) { i ->
        if (i % 3 == 0) {
            add(GameEvent.DeckClaimed(
                claimerId = "p1", claimerName = "Alice",
                teamId = "team_1", halfSuit = HalfSuit.SPADES_LOW, correct = true
            ))
        } else {
            add(GameEvent.CardAsked(
                askerId = "p1", askerName = "Alice",
                targetId = "p2", targetName = "Bob",
                card = Card(Suit.SPADES, CardValue.SEVEN),
                success = i % 2 == 0
            ))
        }
    }
}

private val previewWinState = ResultUiState(
    myTeamScore = 5,
    opponentTeamScore = 3,
    myTeamName = "Team Alpha",
    opponentTeamName = "Team Beta",
    isWinner = true,
    isDraw = false,
    halfSuitBreakdown = previewBreakdown,
    gameLog = previewGameLog
)

private val previewLoseState = previewWinState.copy(
    myTeamScore = 3,
    opponentTeamScore = 5,
    isWinner = false
)

private val previewDrawState = previewWinState.copy(
    myTeamScore = 4,
    opponentTeamScore = 4,
    isWinner = false,
    isDraw = true
)

// ─── Previews ──────────────────────────────────────────────────────────────

@Preview(name = "Result — Win (animated)", showBackground = true)
@Composable
private fun PreviewResultWin() {
    LiteratureTheme {
        ResultScreenContent(
            uiState = previewWinState,
            showLog = false,
            onToggleLog = {},
            onPlayAgain = {},
            onGoHome = {}
        )
    }
}

@Preview(name = "Result — Win, log open", showBackground = true)
@Composable
private fun PreviewResultWinWithLog() {
    LiteratureTheme {
        ResultScreenContent(
            uiState = previewWinState,
            showLog = true,
            onToggleLog = {},
            onPlayAgain = {},
            onGoHome = {}
        )
    }
}

@Preview(name = "Result — Lose", showBackground = true)
@Composable
private fun PreviewResultLose() {
    LiteratureTheme {
        ResultScreenContent(
            uiState = previewLoseState,
            showLog = false,
            onToggleLog = {},
            onPlayAgain = {},
            onGoHome = {}
        )
    }
}

@Preview(name = "Result — Draw", showBackground = true)
@Composable
private fun PreviewResultDraw() {
    LiteratureTheme {
        ResultScreenContent(
            uiState = previewDrawState,
            showLog = false,
            onToggleLog = {},
            onPlayAgain = {},
            onGoHome = {}
        )
    }
}
