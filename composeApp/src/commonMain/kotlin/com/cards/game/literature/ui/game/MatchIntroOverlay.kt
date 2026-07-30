package com.cards.game.literature.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.bot.BotPersonalities
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.viewmodel.PlayerInfo
import kotlinx.coroutines.delay
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.label_opponents
import literature.composeapp.generated.resources.label_your_team
import literature.composeapp.generated.resources.match_intro_first_move_other
import literature.composeapp.generated.resources.match_intro_first_move_you
import literature.composeapp.generated.resources.match_intro_skip_hint
import literature.composeapp.generated.resources.match_intro_vs
import org.jetbrains.compose.resources.stringResource

/** Everything the intro announces, snapshotted from the fresh match state. */
data class MatchIntroData(
    val myTeamLabels: List<String>,
    val opponentLabels: List<String>,
    val firstMoveIsMine: Boolean,
    val firstMoveName: String
)

/** Bots lead with their personality emoji so the reveal matches the board. */
internal fun PlayerInfo.matchIntroLabel(): String =
    if (isBot) "${BotPersonalities.emojiFor(name)} $name" else name

/**
 * The pre-match curtain (~2.6s, tap anywhere to skip): green-vs-red team reveal —
 * lapped once by a gold comet — then who moves first, then it lifts to the live
 * board. The board underneath is real state the whole time — this overlay is
 * purely cosmetic, which keeps it safe against events landing mid-intro (online)
 * and needs no engine changes.
 */
@Composable
fun MatchIntroOverlay(
    data: MatchIntroData,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inPreview = LocalInspectionMode.current

    // Beat machine: 1 = teams slide in, 2 = "goes first" line, 3 = fade out.
    // Saveable so an Activity recreation mid-intro resumes instead of restarting.
    // Under inspection the INITIAL value is already the settled beat — static previews
    // never run LaunchedEffects, so settling in the effect would render frame zero.
    var beat by rememberSaveable { mutableStateOf(if (inPreview) 2 else 0) }
    LaunchedEffect(Unit) {
        if (inPreview) return@LaunchedEffect
        if (beat < 1) beat = 1
        if (beat < 2) {
            delay(1350)
            beat = 2
        }
        delay(850)
        beat = 3
        delay(400)
        onDone()
    }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (beat >= 3) 0f else 1f,
        animationSpec = tween(400),
        label = "introFade"
    )

    // ── Comet lap ────────────────────────────────────────────────────────
    // A gold comet enters from the left at the team panel's top edge, laps the
    // panel once clockwise as a rounded rectangle, and exits right along the same
    // line. Launches with the columns' slide-in and is gone (~1.5s) well before the
    // curtain starts fading (2.2s). The panel's bounds come from layout, so the
    // columns' slide-in (graphicsLayer-only) never moves the frame.
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var teamsWindowBounds by remember { mutableStateOf<Rect?>(null) }
    val cometProgress = remember { Animatable(if (inPreview) 0.55f else 0f) }
    LaunchedEffect(Unit) {
        // beat >= 2 means we resumed near the end (recreation mid-intro) — skip the
        // lap rather than have it run into the curtain's fade-out.
        if (inPreview || beat >= 2) return@LaunchedEffect
        delay(100)
        cometProgress.animateTo(1f, tween(1400, easing = LinearEasing))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = overlayAlpha }
            // Fully opaque, always-dark navy in both themes (the share-card idiom): the
            // app's grounds are navy/white, and green stays reserved for the ours/theirs
            // encoding — a green or translucent curtain would fight both.
            .background(Brush.verticalGradient(listOf(Color(0xFF16213E), Color(0xFF0F0F1E))))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone
            )
            .onGloballyPositioned { overlayOrigin = it.boundsInRoot().topLeft },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.onGloballyPositioned { teamsWindowBounds = it.boundsInRoot() },
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                IntroTeamColumn(
                    header = stringResource(Res.string.label_your_team).uppercase(),
                    members = data.myTeamLabels,
                    accent = LightGreen,
                    headerColor = Color(0xFF7EDC84),
                    visible = beat >= 1,
                    fromLeft = true,
                    delayMillis = 100
                )
                VsMark(visible = beat >= 1)
                IntroTeamColumn(
                    header = stringResource(Res.string.label_opponents).uppercase(),
                    members = data.opponentLabels,
                    accent = CardRed,
                    headerColor = Color(0xFFFF8A80),
                    visible = beat >= 1,
                    fromLeft = false,
                    delayMillis = 250
                )
            }
            Spacer(Modifier.height(30.dp))
            FirstMoveLine(data = data, visible = beat >= 2)
        }
        CometTrail(
            teamsBounds = teamsWindowBounds?.translate(-overlayOrigin),
            progress = { cometProgress.value }
        )
        Text(
            text = stringResource(Res.string.match_intro_skip_hint),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun IntroTeamColumn(
    header: String,
    members: List<String>,
    accent: Color,
    headerColor: Color,
    visible: Boolean,
    fromLeft: Boolean,
    delayMillis: Int
) {
    val offsetX by animateFloatAsState(
        targetValue = if (visible) 0f else if (fromLeft) -60f else 60f,
        animationSpec = tween(500, delayMillis = delayMillis, easing = EaseOutCubic),
        label = "teamSlide"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = delayMillis),
        label = "teamFade"
    )
    Column(
        modifier = Modifier
            // Max width + ellipsis below keep a full 20-char name (the server cap —
            // other players' names arrive at that length regardless of local input)
            // from pushing the two columns off a narrow screen.
            .widthIn(min = 104.dp, max = 148.dp)
            .graphicsLayer {
                translationX = offsetX
                this.alpha = alpha
            },
        verticalArrangement = Arrangement.spacedBy(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = headerColor,
            letterSpacing = 1.5.sp
        )
        members.forEach { label ->
            Row(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).background(accent, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE9EFE7),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun VsMark(visible: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = tween(420, delayMillis = 420, easing = EaseOutBack),
        label = "vsScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300, delayMillis = 420),
        label = "vsFade"
    )
    Text(
        text = stringResource(Res.string.match_intro_vs),
        style = MaterialTheme.typography.displaySmall,
        color = GoldAccent,
        modifier = Modifier
            .padding(top = 22.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
    )
}

/**
 * The comet: a bright gold head trailing a line nearly as long as the frame's
 * perimeter — so mid-lap it reads as a gold border drawing itself around the
 * panel, then whipping away. Enters from the left screen edge exactly at
 * [teamsBounds]' top-edge height, laps the panel clockwise as a rounded rectangle
 * (padded a little so it never touches the chips), and exits off the right edge
 * along the same line. Pure decoration: draws nothing until the panel is measured,
 * and the exit overshoot exceeds the trail length so the line fully drains
 * off-screen instead of popping.
 */
/**
 * Cached comet geometry. The path (4 arcs) and its PathMeasure are expensive to
 * rebuild, so they're constructed once and only refreshed if the frame or canvas
 * width actually changes — the per-frame draw does nothing but segment extraction.
 */
private class CometGeometry {
    private var builtFrame = Rect.Zero
    private var builtWidth = -1f
    private val path = Path()
    val measure = PathMeasure()
    var length = 0f
        private set
    val segment = Path()
    var strokes: Array<Stroke> = emptyArray()
        private set
    var alphas: FloatArray = FloatArray(0)
        private set

    fun ensure(
        frame: Rect,
        canvasWidth: Float,
        corner: Float,
        entryMargin: Float,
        exitMargin: Float,
        chunks: Int,
        minWidth: Float,
        maxWidth: Float
    ) {
        if (frame == builtFrame && canvasWidth == builtWidth && strokes.size == chunks) return
        builtFrame = frame
        builtWidth = canvasWidth
        val top = frame.top
        // Entry + first top pass, clockwise lap, second top pass + exit — the entry
        // and exit segments are collinear with the frame's top edge.
        path.reset()
        path.moveTo(-entryMargin, top)
        path.lineTo(frame.right - corner, top)
        path.arcTo(Rect(frame.right - 2 * corner, top, frame.right, top + 2 * corner), -90f, 90f, false)
        path.lineTo(frame.right, frame.bottom - corner)
        path.arcTo(Rect(frame.right - 2 * corner, frame.bottom - 2 * corner, frame.right, frame.bottom), 0f, 90f, false)
        path.lineTo(frame.left + corner, frame.bottom)
        path.arcTo(Rect(frame.left, frame.bottom - 2 * corner, frame.left + 2 * corner, frame.bottom), 90f, 90f, false)
        path.lineTo(frame.left, top + corner)
        path.arcTo(Rect(frame.left, top, frame.left + 2 * corner, top + 2 * corner), 180f, 90f, false)
        path.lineTo(canvasWidth + exitMargin, top)
        measure.setPath(path, forceClosed = false)
        length = measure.length
        // Butt caps: adjacent chunks share exact endpoints, so the translucent
        // strokes never double-paint at the seams (round caps overlapped there and
        // read as a row of bright beads along the line).
        strokes = Array(chunks) { i ->
            val f = 1f - i.toFloat() / chunks
            Stroke(width = minWidth + (maxWidth - minWidth) * f, cap = StrokeCap.Butt)
        }
        alphas = FloatArray(chunks) { i -> 0.8f * (1f - i.toFloat() / chunks) }
    }
}

@Composable
private fun CometTrail(teamsBounds: Rect?, progress: () -> Float, modifier: Modifier = Modifier) {
    if (teamsBounds == null) return
    val geometry = remember { CometGeometry() }
    Canvas(modifier = modifier.fillMaxSize()) {
        // Progress is read HERE, in the draw phase: each animation frame invalidates
        // only this draw pass. Reading it during composition (as a plain parameter)
        // recomposed the entire overlay at 60fps — a visible stutter source.
        val p = progress()
        if (p <= 0f || p >= 1f) return@Canvas

        val pad = 12.dp.toPx()
        val frame = Rect(
            teamsBounds.left - pad,
            teamsBounds.top - pad,
            teamsBounds.right + pad,
            teamsBounds.bottom + pad
        )
        val tailLen = 0.85f * 2f * (frame.width + frame.height)
        val chunks = 24
        geometry.ensure(
            frame = frame,
            canvasWidth = size.width,
            corner = 16.dp.toPx(),
            entryMargin = 32.dp.toPx(),
            exitMargin = tailLen + 32.dp.toPx(),
            chunks = chunks,
            minWidth = 0.8.dp.toPx(),
            maxWidth = 4.dp.toPx()
        )
        val headDist = p * geometry.length
        val chunkLen = tailLen / chunks
        for (i in chunks - 1 downTo 0) {
            val start = headDist - (i + 1) * chunkLen
            val stop = headDist - i * chunkLen
            if (stop <= 0f) continue
            geometry.segment.reset()
            if (!geometry.measure.getSegment(start.coerceAtLeast(0f), stop, geometry.segment, true)) continue
            drawPath(
                path = geometry.segment,
                color = GoldAccent,
                alpha = geometry.alphas[i],
                style = geometry.strokes[i]
            )
        }
        val head = geometry.measure.getPosition(headDist)
        drawCircle(color = GoldAccent, radius = 9.dp.toPx(), center = head, alpha = 0.22f)
        drawCircle(color = lerp(GoldAccent, Color.White, 0.35f), radius = 3.5.dp.toPx(), center = head)
    }
}

@Composable
private fun FirstMoveLine(data: MatchIntroData, visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400),
        label = "firstMoveFade"
    )
    val rise by animateFloatAsState(
        targetValue = if (visible) 0f else 16f,
        animationSpec = tween(450, easing = EaseOutCubic),
        label = "firstMoveRise"
    )
    Text(
        text = "✦ " + if (data.firstMoveIsMine) {
            stringResource(Res.string.match_intro_first_move_you)
        } else {
            stringResource(Res.string.match_intro_first_move_other, data.firstMoveName)
        },
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = GoldAccent,
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = rise
        }
    )
}
