package com.cards.game.literature.ui.game.tutorial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

// ─── Tutorial Step ──────────────────────────────────────────────────────────

enum class TutorialStep {
    SCORE_BAR,
    TURN_BANNER,
    PLAYERS,
    HALF_SUITS,
    HAND_TAB,       // Waits for user to tap the Hand tab
    YOUR_HAND,      // Shows on Hand tab, highlighting the cards
    ACTION_BUTTONS;

    fun next(): TutorialStep? = entries.getOrNull(ordinal + 1)

    /** True if this step requires a user action (not just a tap anywhere). */
    val requiresUserAction: Boolean get() = this == HAND_TAB
}

private fun TutorialStep.emoji(): String = when (this) {
    TutorialStep.SCORE_BAR -> "🏆"
    TutorialStep.TURN_BANNER -> "⏱"
    TutorialStep.PLAYERS -> "👥"
    TutorialStep.HALF_SUITS -> "🃏"
    TutorialStep.HAND_TAB -> "👆"
    TutorialStep.YOUR_HAND -> "🎴"
    TutorialStep.ACTION_BUTTONS -> "✨"
}

// ─── Tutorial State ─────────────────────────────────────────────────────────

class TutorialState(initiallyActive: Boolean) {
    var isActive by mutableStateOf(initiallyActive)
        private set
    var currentStep by mutableStateOf(TutorialStep.SCORE_BAR)
        private set

    val targetBounds = mutableStateMapOf<TutorialStep, Rect>()

    fun advance() {
        val next = currentStep.next()
        if (next != null) {
            currentStep = next
        } else {
            isActive = false
        }
    }

    fun reportBounds(step: TutorialStep, rect: Rect) {
        targetBounds[step] = rect
    }
}

@Composable
fun rememberTutorialState(isFirstGame: Boolean): TutorialState {
    return remember { TutorialState(initiallyActive = isFirstGame) }
}

// ─── Tutorial Overlay ───────────────────────────────────────────────────────

@Composable
fun TutorialOverlay(state: TutorialState) {
    if (!state.isActive) return

    val bounds = state.targetBounds[state.currentStep]
    val density = LocalDensity.current
    val accentColor = MaterialTheme.colorScheme.secondary

    // Pulse animation for the spotlight border
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // On HAND_TAB step, don't consume taps — let the nav bar handle them
    val consumesTaps = !state.currentStep.requiresUserAction

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (consumesTaps) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { state.advance() }
                    )
                } else Modifier
            )
    ) {
        // Semi-transparent scrim with spotlight cutout
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.7f))

            if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
                val pad = 10.dp.toPx()
                val spotlightLeft = bounds.left - pad
                val spotlightTop = bounds.top - pad
                val spotlightWidth = bounds.width + pad * 2
                val spotlightHeight = bounds.height + pad * 2
                val corner = CornerRadius(12.dp.toPx())

                // Cut out spotlight
                drawRoundRect(
                    color = Color.Black,
                    topLeft = androidx.compose.ui.geometry.Offset(spotlightLeft, spotlightTop),
                    size = androidx.compose.ui.geometry.Size(spotlightWidth, spotlightHeight),
                    cornerRadius = corner,
                    blendMode = BlendMode.DstOut
                )

                // Pulsing accent border
                drawRoundRect(
                    color = Color(accentColor.red, accentColor.green, accentColor.blue, pulseAlpha),
                    topLeft = androidx.compose.ui.geometry.Offset(spotlightLeft, spotlightTop),
                    size = androidx.compose.ui.geometry.Size(spotlightWidth, spotlightHeight),
                    cornerRadius = corner,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
                )

                // Pulsing fill glow on steps that require user action (e.g. HAND_TAB)
                if (state.currentStep.requiresUserAction) {
                    drawRoundRect(
                        color = Color(accentColor.red, accentColor.green, accentColor.blue, pulseAlpha * 0.25f),
                        topLeft = androidx.compose.ui.geometry.Offset(spotlightLeft, spotlightTop),
                        size = androidx.compose.ui.geometry.Size(spotlightWidth, spotlightHeight),
                        cornerRadius = corner,
                    )
                }
            }
        }

        // Tooltip bubble with triangle pointer
        if (bounds != null && bounds.width > 0f) {
            val showAbove = state.currentStep == TutorialStep.ACTION_BUTTONS
                    || state.currentStep == TutorialStep.HAND_TAB
                    || state.currentStep == TutorialStep.HALF_SUITS
                    || state.currentStep == TutorialStep.YOUR_HAND

            val spotlightPad = with(density) { 10.dp.toPx() }
            val triangleH = with(density) { 10.dp.toPx() }
            val gapPx = with(density) { 6.dp.toPx() }
            val triangleCenterX = bounds.center.x

            // Anchor: the y-coordinate where the triangle tip touches
            val anchorY = if (showAbove) {
                bounds.top - spotlightPad - gapPx
            } else {
                bounds.bottom + spotlightPad + gapPx
            }

            // Triangle pointer
            Canvas(modifier = Modifier.fillMaxSize()) {
                val triW = 14.dp.toPx()
                val path = Path().apply {
                    if (showAbove) {
                        // Tip points down at anchorY, body extends up
                        moveTo(triangleCenterX, anchorY)
                        lineTo(triangleCenterX - triW, anchorY - triangleH)
                        lineTo(triangleCenterX + triW, anchorY - triangleH)
                    } else {
                        // Tip points up at anchorY, body extends down
                        moveTo(triangleCenterX, anchorY)
                        lineTo(triangleCenterX - triW, anchorY + triangleH)
                        lineTo(triangleCenterX + triW, anchorY + triangleH)
                    }
                    close()
                }
                drawPath(path, color = Color.White)
            }

            // Tooltip card — use a Box that fills the space above/below the spotlight,
            // then align the card to the edge nearest the spotlight so it never overlaps.
            if (showAbove) {
                // Card fills area from top of screen to triangle base, aligned to bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(density) { (anchorY - triangleH).toDp() }),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    TooltipCard(state = state, accentColor = accentColor)
                }
            } else {
                // Card fills area from triangle base to bottom of screen, aligned to top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, (anchorY + triangleH).toInt()) },
                    contentAlignment = Alignment.TopCenter
                ) {
                    TooltipCard(state = state, accentColor = accentColor)
                }
            }
        }
    }
}

@Composable
private fun TooltipCard(state: TutorialState, accentColor: Color) {
    val hintText = if (state.currentStep.requiresUserAction) {
        stringResource(Res.string.tutorial_tap_hand_tab)
    } else {
        stringResource(Res.string.tutorial_tap_continue)
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 400.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(top = 16.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step counter
        Text(
            text = "${state.currentStep.ordinal + 1} / ${TutorialStep.entries.size}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Emoji
        Text(
            text = state.currentStep.emoji(),
            fontSize = 28.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        Text(
            text = tooltipTextForStep(state.currentStep),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1B1F),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Tap hint
        Text(
            text = hintText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = Color(accentColor.red, accentColor.green, accentColor.blue),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun tooltipTextForStep(step: TutorialStep): String = when (step) {
    TutorialStep.SCORE_BAR -> stringResource(Res.string.tutorial_score_bar)
    TutorialStep.TURN_BANNER -> stringResource(Res.string.tutorial_turn_banner)
    TutorialStep.PLAYERS -> stringResource(Res.string.tutorial_players)
    TutorialStep.HALF_SUITS -> stringResource(Res.string.tutorial_half_suits)
    TutorialStep.HAND_TAB -> stringResource(Res.string.tutorial_hand_tab)
    TutorialStep.YOUR_HAND -> stringResource(Res.string.tutorial_your_hand)
    TutorialStep.ACTION_BUTTONS -> stringResource(Res.string.tutorial_action_buttons)
}
