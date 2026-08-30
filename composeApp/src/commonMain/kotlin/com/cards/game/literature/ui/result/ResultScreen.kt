package com.cards.game.literature.ui.result

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import com.cards.game.literature.analytics.Analytics
import com.cards.game.literature.analytics.AnalyticsEvent
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import com.cards.game.literature.model.isLow
import com.cards.game.literature.model.isRed
import com.cards.game.literature.model.suit
import androidx.compose.foundation.border
import com.cards.game.literature.stats.Achievement
import com.cards.game.literature.ui.common.BackHandler
import com.cards.game.literature.ui.game.GameLogScreen
import com.cards.game.literature.ui.stats.AchievementUnlockCard
import com.cards.game.literature.ui.stats.ui
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.viewmodel.ResultUiState
import com.cards.game.literature.viewmodel.ResultViewModel
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.cards.game.literature.deeplink.InviteLink
import com.cards.game.literature.share.Sharer
import com.cards.game.literature.share.imageBitmapToPng
import kotlinx.coroutines.launch
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.achievement_unlocked_banner
import literature.composeapp.generated.resources.achievement_unlocked_more
import literature.composeapp.generated.resources.button_home
import literature.composeapp.generated.resources.button_play_again
import literature.composeapp.generated.resources.button_rematch
import literature.composeapp.generated.resources.button_share
import literature.composeapp.generated.resources.label_opponents
import literature.composeapp.generated.resources.label_your_team
import literature.composeapp.generated.resources.result_breakdown_title
import literature.composeapp.generated.resources.result_debrief_body
import literature.composeapp.generated.resources.result_debrief_title
import literature.composeapp.generated.resources.result_draw
import literature.composeapp.generated.resources.result_lose
import literature.composeapp.generated.resources.ask_filter_high
import literature.composeapp.generated.resources.ask_filter_low
import literature.composeapp.generated.resources.cd_deck_open
import literature.composeapp.generated.resources.cd_deck_ours
import literature.composeapp.generated.resources.cd_deck_ours_stolen
import literature.composeapp.generated.resources.cd_deck_theirs
import literature.composeapp.generated.resources.cd_deck_theirs_stolen
import literature.composeapp.generated.resources.deck_tracker_open
import literature.composeapp.generated.resources.deck_tracker_ours
import literature.composeapp.generated.resources.deck_tracker_theirs
import literature.composeapp.generated.resources.result_show_log
import literature.composeapp.generated.resources.result_steal_hint
import literature.composeapp.generated.resources.result_win
import literature.composeapp.generated.resources.result_share
import literature.composeapp.generated.resources.share_result_caption
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.random.Random
import com.cards.game.literature.ui.common.emoji

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

// ─── Half-suit breakdown: suit-chip grid ───────────────────────────────────

/** Ownership of a half-suit from the local player's view: 1 = ours, 2 = theirs, 0 = open.
 *  Online players can be on team 2, so this compares against the real [myTeamId], never "team_1". */
private fun HalfSuitStatus.owner(myTeamId: String): Int = when (claimedByTeamId) {
    null -> 0
    myTeamId -> 1
    else -> 2
}

/** A set is "stolen" when it was awarded to its owner by the OTHER team's failed claim
 *  (claimCorrect == false) — the most dramatic beat in a game, marked with a ⚡. */
private val HalfSuitStatus.isStolen: Boolean get() = claimCorrect == false

/** Shared accessibility label for a half-suit cell — earned/stolen aware. */
@Composable
private fun deckContentDescription(status: HalfSuitStatus, myTeamId: String): String {
    val name = status.halfSuit.displayName
    return when (status.owner(myTeamId)) {
        1 -> if (status.isStolen) stringResource(Res.string.cd_deck_ours_stolen, name)
             else stringResource(Res.string.cd_deck_ours, name)
        2 -> if (status.isStolen) stringResource(Res.string.cd_deck_theirs_stolen, name)
             else stringResource(Res.string.cd_deck_theirs, name)
        else -> stringResource(Res.string.cd_deck_open, name)
    }
}

/** The eight half-suits as a 4×2 tinted chip grid — the whole breakdown.
 *  Reuses the in-game DeckTracker vocabulary (suit glyph, Low/High, ownership tag);
 *  a ⚡ marks any set won on the opponents' failed claim. */
@Composable
private fun SuitChipGrid(
    statuses: List<HalfSuitStatus>,
    myTeamId: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        statuses.chunked(4).forEachIndexed { rowIndex, rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                rowItems.forEachIndexed { colIndex, status ->
                    SuitChipCell(
                        status = status,
                        myTeamId = myTeamId,
                        index = rowIndex * 4 + colIndex,
                        visible = visible,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Pad a short final row so remaining cells keep their column width.
                repeat(4 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        // Footnote: only when a set changed hands on a failed claim. Fades in just after
        // the last chip (delay ≈ the 8th cell's) so it never pops ahead of the grid.
        if (statuses.any { it.isStolen }) {
            val hintAlpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(300, delayMillis = if (visible) 560 else 0, easing = EaseOut),
                label = "stealHintAlpha"
            )
            Text(
                text = "⚡ " + stringResource(Res.string.result_steal_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp)
                    .graphicsLayer { alpha = hintAlpha }
            )
        }
    }
}

@Composable
private fun SuitChipCell(
    status: HalfSuitStatus,
    myTeamId: String,
    index: Int,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300, delayMillis = if (visible) 200 + index * 45 else 0, easing = EaseOut),
        label = "chipAlpha$index"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 16f,
        animationSpec = tween(300, delayMillis = if (visible) 200 + index * 45 else 0, easing = EaseOutBack),
        label = "chipOffset$index"
    )

    val owner = status.owner(myTeamId)
    val bg = when (owner) {
        1 -> LightGreen.copy(alpha = 0.20f)
        2 -> CardRed.copy(alpha = 0.20f)
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
    }
    val border = when (owner) {
        1 -> LightGreen
        2 -> CardRed
        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    }
    val ownLabel = when (owner) {
        1 -> stringResource(Res.string.deck_tracker_ours)
        2 -> stringResource(Res.string.deck_tracker_theirs)
        else -> stringResource(Res.string.deck_tracker_open)
    }
    val ownColor = when (owner) {
        1 -> LightGreen
        2 -> CardRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val suit = status.halfSuit.suit
    val suitColor = if (suit.isRed) CardRed else MaterialTheme.colorScheme.onSurface
    val lowHigh = if (status.halfSuit.isLow) stringResource(Res.string.ask_filter_low)
                  else stringResource(Res.string.ask_filter_high)
    val cd = deckContentDescription(status, myTeamId)

    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha; translationY = offsetY }
            .clearAndSetSemantics { contentDescription = cd }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bg, RoundedCornerShape(9.dp))
                .border(1.dp, border, RoundedCornerShape(9.dp))
                .padding(vertical = 8.dp, horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(suit.emoji, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = suitColor)
            Text(
                lowHigh.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                ownLabel,
                style = MaterialTheme.typography.labelSmall,
                color = ownColor,
                fontWeight = FontWeight.Bold
            )
        }
        if (status.isStolen) {
            Text("⚡", fontSize = 11.sp, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}

// ─── Screens ──────────────────────────────────────────────────────────────

@Composable
fun ResultScreen(
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    onRematchNavigate: (roomCode: String) -> Unit = {},
    viewModel: ResultViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLog by remember { mutableStateOf(false) }

    // Host pressed Rematch (or we're a guest and the host did) — the server
    // confirmed by resetting the room, so everyone returns to the waiting room.
    LaunchedEffect(Unit) {
        viewModel.rematchStarted.collect {
            onRematchNavigate(viewModel.roomCode)
        }
    }

    ResultScreenContent(
        uiState = uiState,
        showLog = showLog,
        onToggleLog = { showLog = !showLog },
        onPlayAgain = onPlayAgain,
        onGoHome = onGoHome,
        onRematch = viewModel::requestRematch
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreenContent(
    uiState: ResultUiState,
    showLog: Boolean,
    onToggleLog: () -> Unit,
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    onRematch: () -> Unit = {}
) {
    // In a @Preview the LaunchedEffect entrance animations never fire, so start the reveal
    // flags already-visible under inspection — the preview then shows the full screen
    // (banner + breakdown rows + achievements) instead of a half-empty one.
    val inInspection = LocalInspectionMode.current

    // The win/lose/draw sound + haptics fire at the on-board finale stinger
    // (MatchStingerOverlay), so the audio lands on the word punch — nothing may
    // replay them here.

    // Reveal flags are SAVEABLE: an Activity recreation (system theme toggle is a
    // uiMode config change) restores them as true, so the entrance choreography
    // plays once per arrival instead of replaying on every recreation.
    // ── Banner entrance: visible after short delay ────────────────────────
    var bannerVisible by rememberSaveable { mutableStateOf(inInspection) }
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
    var breakdownVisible by rememberSaveable { mutableStateOf(inInspection) }
    LaunchedEffect(Unit) {
        delay(800)
        breakdownVisible = true
    }

    // ── Win confetti: a one-arrival flourish ──────────────────────────────
    // The saveable flag survives recreation; the plain remember freezes the decision
    // for this composition so flipping the flag doesn't cut the rain off mid-fall.
    var confettiSpent by rememberSaveable { mutableStateOf(false) }
    val playConfetti = remember { !confettiSpent }
    LaunchedEffect(Unit) { confettiSpent = true }

    // ── Share nudge: one gentle pulse once the reveal has settled ─────────
    // The result moment is the share moment; saveable so a recreation can't re-pulse.
    var sharePulseSpent by rememberSaveable { mutableStateOf(false) }
    val sharePulseScale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (sharePulseSpent || inInspection) return@LaunchedEffect
        delay(2600)
        sharePulseSpent = true
        repeat(2) {
            sharePulseScale.animateTo(1.08f, tween(170, easing = EaseOut))
            sharePulseScale.animateTo(1f, tween(220))
        }
    }

    val myTeamDisplayName = uiState.myTeamName.ifEmpty { stringResource(Res.string.label_your_team) }
    val opponentTeamDisplayName = uiState.opponentTeamName.ifEmpty { stringResource(Res.string.label_opponents) }

    // ── Shareable result card capture ─────────────────────────────────────
    val shareLayer = rememberGraphicsLayer()
    val shareScope = rememberCoroutineScope()
    val shareCaption = stringResource(Res.string.share_result_caption, InviteLink.PLAY_STORE)

    // safeDrawingPadding is on the Box (outside the scroll) so it frames the viewport as a fixed
    // inset — inside verticalScroll it would scroll away and let content slide under the status
    // bar. It also makes maxHeight the safe-area height, so the centering math below stays right.
    BoxWithConstraints(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // min = the safe-area viewport height, so short results stay vertically centered
                // but taller ones (first-game debrief + achievements) grow and scroll — clipped
                // to the safe area, never under the status bar.
                .heightIn(min = maxHeight)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Winner banner ─────────────────────────────────────────────
            // Space is reserved from the first frame (graphicsLayer reveal, not
            // AnimatedVisibility): the banner entering must not reflow the column
            // and shove the chrome below it around while the screen settles.
            val bannerAlpha by animateFloatAsState(
                targetValue = if (bannerVisible) 1f else 0f,
                animationSpec = tween(300),
                label = "bannerAlpha"
            )
            val bannerEnterScale by animateFloatAsState(
                targetValue = if (bannerVisible) 1f else 0.6f,
                animationSpec = tween(500, easing = EaseOutBack),
                label = "bannerEnterScale"
            )
            val bannerDrop by animateFloatAsState(
                targetValue = if (bannerVisible) 0f else -60f,
                animationSpec = tween(500, easing = EaseOutBack),
                label = "bannerDrop"
            )
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
                modifier = Modifier.graphicsLayer {
                    alpha = bannerAlpha
                    translationY = bannerDrop
                    val s = bannerEnterScale * (if (uiState.isWinner) bannerScale else 1f)
                    scaleX = s
                    scaleY = s
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Score ─────────────────────────────────────────────────────
            // lnum = lining figures: Playfair's default old-style figures give digits uneven
            // heights (6/8 sit tall, 0/1/2 short), so a score like "6 - 2" reads lopsided. lnum
            // makes them uniform cap-height while keeping the Playfair display face.
            val scoreStyle = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "lnum")
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
                        style = scoreStyle,
                        fontWeight = FontWeight.Bold,
                        color = LightGreen
                    )
                }
                Text(
                    "-",
                    style = scoreStyle,
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
                        style = scoreStyle,
                        fontWeight = FontWeight.Bold,
                        color = CardRed
                    )
                }
            }

            // ── First-game debrief (teaching layer, shown once) ───────────
            if (uiState.isFirstGame) {
                Spacer(modifier = Modifier.height(20.dp))
                FirstGameDebriefCard()
            }

            // ── Achievement unlocks ───────────────────────────────────────
            if (uiState.unlockedAchievements.isNotEmpty()) {
                var achievementsVisible by rememberSaveable { mutableStateOf(inInspection) }
                LaunchedEffect(Unit) {
                    delay(1200) // let the score count-up land first
                    achievementsVisible = true
                }
                val achievementsAlpha by animateFloatAsState(
                    targetValue = if (achievementsVisible) 1f else 0f,
                    animationSpec = tween(300),
                    label = "achievementsAlpha"
                )
                val achievementsScale by animateFloatAsState(
                    targetValue = if (achievementsVisible) 1f else 0.8f,
                    animationSpec = tween(450, easing = EaseOutBack),
                    label = "achievementsScale"
                )
                val achievementsRise by animateFloatAsState(
                    targetValue = if (achievementsVisible) 0f else 48f,
                    animationSpec = tween(450, easing = EaseOutBack),
                    label = "achievementsRise"
                )
                Spacer(modifier = Modifier.height(20.dp))
                // Space reserved from the first frame — this card entering mid-reveal
                // must not shove the breakdown down the screen.
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = achievementsAlpha
                        translationY = achievementsRise
                        scaleX = achievementsScale
                        scaleY = achievementsScale
                    }
                ) {
                    AchievementUnlockCard(uiState.unlockedAchievements)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Breakdown ─────────────────────────────────────────────────
            // Title + surface card fade in WITH the chip reveal: an empty container
            // arriving ahead of its content reads as broken chrome, especially on the
            // light theme's bright background. Alpha, not visibility — space stays
            // reserved so nothing below shifts.
            val breakdownAlpha by animateFloatAsState(
                targetValue = if (breakdownVisible) 1f else 0f,
                animationSpec = tween(350),
                label = "breakdownAlpha"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = breakdownAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(Res.string.result_breakdown_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    SuitChipGrid(
                        statuses = uiState.halfSuitBreakdown,
                        myTeamId = uiState.myTeamId,
                        visible = breakdownVisible
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Primary action ────────────────────────────────────────────
            // Online host gets Rematch (same room, same players); everyone
            // else keeps the local Play Again behavior.
            Button(
                onClick = if (uiState.canRematch) onRematch else onPlayAgain,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    stringResource(if (uiState.canRematch) Res.string.button_rematch else Res.string.button_play_again),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Secondary actions: Share + Home side by side ──────────────
            Row(
                modifier = Modifier.fillMaxWidth(0.7f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tight content padding + single-line labels so the icon + text
                // fit without wrapping ("Hom"/"e") at the narrow weight(1f) width.
                OutlinedButton(
                    onClick = {
                        Analytics.log(AnalyticsEvent.ResultShared)
                        shareScope.launch {
                            val bitmap = shareLayer.toImageBitmap()
                            Sharer.shareImage(imageBitmapToPng(bitmap), shareCaption)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp).scale(sharePulseScale.value),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.button_share), maxLines = 1)
                }
                OutlinedButton(
                    onClick = onGoHome,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.button_home), maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Game log (opens in a bottom sheet — see below) ────────────
            TextButton(onClick = onToggleLog) {
                Text(
                    stringResource(Res.string.result_show_log),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Game log opens as a full-screen page (GameLogScreen), rendered below after the confetti.

        // ── Off-screen shareable card, captured on demand into shareLayer ──
        // Pushed far off-screen (not size 0 / alpha 0) so it's measured & drawn —
        // and thus recorded — but never visible. Fixed density => ~1080px PNG on
        // any device, regardless of the user's light/dark setting.
        CompositionLocalProvider(LocalDensity provides Density(density = 3f, fontScale = 1f)) {
            LiteratureTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        // 4:5 portrait (~1080x1350 at 3x) — must match the aspect the
                        // card is laid out/previewed at, or SpaceBetween crams the blocks.
                        .size(width = 360.dp, height = 450.dp)
                        .offset(x = 10_000.dp)
                        .drawWithContent {
                            // record (not draw) — populates the layer for toImageBitmap();
                            // the box is off-screen so nothing needs to paint to the canvas.
                            shareLayer.record { this@drawWithContent.drawContent() }
                        }
                ) {
                    ResultShareCard(uiState, Modifier.fillMaxSize())
                }
            }
        }

        // ── Confetti (win only) ────────────────────────────────────────────
        // Stays COMPOSED while the log page covers it — gating on !showLog
        // unmounted the overlay, and its one-shot rain restarted on every log
        // round-trip. Composed once, the rain plays once and finishes invisible
        // (alpha reaches 0 at full progress), never invalidating again.
        if (uiState.isWinner && playConfetti) {
            ConfettiOverlay()
        }

        // ── Game log — a full-screen page over the result ─────────────────
        if (showLog) {
            BackHandler { onToggleLog() }
            GameLogScreen(events = uiState.gameLog, onClose = onToggleLog)
        }
    }
}

// ─── Preview data ──────────────────────────────────────────────────────────

private val previewBreakdown = listOf(
    HalfSuitStatus(HalfSuit.SPADES_LOW, claimedByTeamId = "team_1", claimCorrect = true),
    HalfSuitStatus(HalfSuit.SPADES_HIGH, claimedByTeamId = "team_1", claimCorrect = true),
    HalfSuitStatus(HalfSuit.HEARTS_LOW, claimedByTeamId = "team_2", claimCorrect = true),
    HalfSuitStatus(HalfSuit.HEARTS_HIGH, claimedByTeamId = "team_2", claimCorrect = true),
    HalfSuitStatus(HalfSuit.DIAMONDS_LOW, claimedByTeamId = "team_1", claimCorrect = true),
    // Awarded to us by team_2's failed claim → "stolen" (⚡).
    HalfSuitStatus(HalfSuit.DIAMONDS_HIGH, claimedByTeamId = "team_1", claimCorrect = false),
    HalfSuitStatus(HalfSuit.CLUBS_LOW, claimedByTeamId = "team_1", claimCorrect = true),
    HalfSuitStatus(HalfSuit.CLUBS_HIGH, claimedByTeamId = "team_2", claimCorrect = true),
)

/** One-time teaching card shown on the result of a player's very first game — reinforces the
 *  win condition right after they've lived it. Gated by [ResultUiState.isFirstGame]. */
@Composable
private fun FirstGameDebriefCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("💡", fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
        Column {
            Text(
                text = stringResource(Res.string.result_debrief_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.result_debrief_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
        }
    }
}

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
    myTeamId = "team_1",
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

@Preview(name = "Result — Win with achievements", showBackground = true)
@Composable
private fun PreviewResultWinWithAchievements() {
    LiteratureTheme {
        ResultScreenContent(
            uiState = previewWinState.copy(
                unlockedAchievements = listOf(Achievement.FIRST_WIN, Achievement.CLAIM_MASTER)
            ),
            showLog = false,
            onToggleLog = {},
            onPlayAgain = {},
            onGoHome = {}
        )
    }
}

// More than MAX_UNLOCKS_SHOWN unlocks at once — exercises the "+N more" cap.
// (Renders statically now — inspection mode starts the reveal flags visible.)
@Preview(name = "Result — Win, capped achievements", showBackground = true)
@Composable
private fun PreviewResultWinWithManyAchievements() {
    LiteratureTheme {
        ResultScreenContent(
            uiState = previewWinState.copy(
                unlockedAchievements = listOf(
                    Achievement.FIRST_WIN,
                    Achievement.HAT_TRICK,
                    Achievement.ON_FIRE,
                    Achievement.PERFECT_GAME,
                    Achievement.CLAIM_MASTER
                )
            ),
            showLog = false,
            onToggleLog = {},
            onPlayAgain = {},
            onGoHome = {}
        )
    }
}

// Isolated unlock card with 5 unlocks → renders "3 rows + '+2 more'" immediately
// (no animation gate), so the cap is visible in a static preview.
@Preview(name = "Achievement card — capped (+N more)", showBackground = true)
@Composable
private fun PreviewAchievementUnlockCardCapped() {
    LiteratureTheme {
        Box(modifier = Modifier.padding(24.dp)) {
            AchievementUnlockCard(
                listOf(
                    Achievement.FIRST_WIN,
                    Achievement.HAT_TRICK,
                    Achievement.ON_FIRE,
                    Achievement.PERFECT_GAME,
                    Achievement.CLAIM_MASTER
                )
            )
        }
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

// The one to open: first-game debrief + several achievements + full breakdown, all at once.
@Preview(name = "Result — First game (debrief + achievements)", showBackground = true)
@Composable
private fun PreviewResultFirstGameFull() {
    LiteratureTheme {
        ResultScreenContent(
            uiState = previewWinState.copy(
                isFirstGame = true,
                unlockedAchievements = listOf(
                    Achievement.FIRST_WIN,
                    Achievement.HAT_TRICK,
                    Achievement.CLAIM_MASTER
                )
            ),
            showLog = false,
            onToggleLog = {},
            onPlayAgain = {},
            onGoHome = {}
        )
    }
}

// Same content forced into a short viewport so it overflows — exercises the scroll/centering
// fix: the top (banner/score) stays reachable and top-anchored instead of clipping. (Preview
// panes have no system-bar insets, so verify the status-bar clearance on-device.)
@Preview(name = "Result — First game (short screen, scrolls)", showBackground = true, heightDp = 640)
@Composable
private fun PreviewResultFirstGameShort() {
    LiteratureTheme {
        ResultScreenContent(
            uiState = previewWinState.copy(
                isFirstGame = true,
                unlockedAchievements = listOf(
                    Achievement.FIRST_WIN,
                    Achievement.HAT_TRICK,
                    Achievement.CLAIM_MASTER
                )
            ),
            showLog = false,
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
