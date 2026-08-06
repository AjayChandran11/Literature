package com.cards.game.literature.festival

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.stats.currentEpochDay
import com.cards.game.literature.ui.theme.LiteratureTheme
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

enum class Festival {
    INDEPENDENCE_DAY,
    // Future: ONAM, DIWALI, PONGAL — add each one's region + windows in FestivalCalendar.
}

object FestivalCalendar {
    // Independence Day is India-specific and the app ships in many countries, so gate on the
    // device region. This is a proxy for "in India", not citizenship — the best signal available.
    private fun inIndia(): Boolean = Locale.current.region == "IN"

    /** The festival whose ACCENT window covers [epochDay] (default today), or null. The tricolour
     *  wordmark accent runs Aug 15–20 — nothing shows before the festival day; the greeting is
     *  shorter, Aug 15 only (see [bannerActive]). */
    fun active(epochDay: Long = currentEpochDay()): Festival? {
        if (!inIndia()) return null
        val (_, m, d) = civilFromDays(epochDay)
        return if (m == 8 && d in 15..20) Festival.INDEPENDENCE_DAY else null
    }

    /** The greeting shows only on the day itself (Aug 15). */
    fun bannerActive(epochDay: Long = currentEpochDay()): Boolean {
        if (!inIndia()) return false
        val (_, m, d) = civilFromDays(epochDay)
        return m == 8 && d == 15
    }

    // Hinnant's civil_from_days: epoch day (days since 1970-01-01) -> (year, month, day). Pure
    // integer math, so no kotlinx-datetime dependency. Year is unused for annual festivals.
    private fun civilFromDays(z0: Long): Triple<Int, Int, Int> {
        val z = z0 + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = if (mp < 10) mp + 3 else mp - 9
        return Triple((y + if (m <= 2) 1L else 0L).toInt(), m.toInt(), d.toInt())
    }
}

// India flag palette.
private val Saffron = Color(0xFFFF9933)
private val IndiaGreen = Color(0xFF138808)

/**
 * Wordmark in the flag tricolour, drawn in two passes — a soft, theme-aware stroke outline behind,
 * the saffron→white→green fill on top — so the white middle band stays legible on both themes.
 * The fill is animated: the tricolour gently slides left↔right under the letters, a slow flag
 * ripple so the wordmark isn't static during the festival. Isolated here so only the wordmark
 * repaints each frame.
 */
@Composable
fun TricolorWordmark(
    text: String,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null
) {
    val outlineWidth = with(LocalDensity.current) { 2.dp.toPx() }
    // Soft + theme-aware: onBackground contrasts with the Home background either way, so at low
    // alpha it reads as a gentle LIGHT edge on the dark theme and a soft edge on the light theme.
    val outlineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)

    // Static tricolour fill (readable) + a soft white highlight that travels across ONE direction
    // every few seconds — light catching a flag, no back-and-forth. The travel multiplier pushes
    // the highlight off-screen for most of the cycle, so there's a gentle pause between passes.
    var widthPx by remember { mutableStateOf(0) }
    val transition = rememberInfiniteTransition(label = "shine")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )
    val w = widthPx.toFloat()
    val band = w * 0.30f
    val glintX = -band + sweep * (w + band) * 2.4f
    val fillBrush = Brush.horizontalGradient(listOf(Saffron, Color.White, IndiaGreen))
    val glintBrush = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.65f), Color.Transparent),
        start = Offset(glintX, 0f),
        end = Offset(glintX + band, 0f)
    )

    Box(
        modifier = modifier.onSizeChanged { if (it.width > 0) widthPx = it.width },
        contentAlignment = Alignment.Center
    ) {
        // Outline behind.
        Text(
            text = text,
            fontSize = fontSize,
            textAlign = textAlign,
            style = baseStyle.copy(
                color = outlineColor,
                drawStyle = Stroke(width = outlineWidth, join = StrokeJoin.Round)
            )
        )
        // Static tricolour fill.
        Text(
            text = text,
            fontSize = fontSize,
            textAlign = textAlign,
            style = baseStyle.copy(brush = fillBrush)
        )
        // Travelling highlight on top.
        Text(
            text = text,
            fontSize = fontSize,
            textAlign = textAlign,
            style = baseStyle.copy(brush = glintBrush)
        )
    }
}

/** A compact greeting for Aug 15 — no background (so it never collides with the Home corner
 *  watermarks), sits inline within the hero text, and fades + slides in on appear. */
@Composable
fun IndependenceDayGreeting(modifier: Modifier = Modifier) {
    // Start visible in a @Preview (entrance LaunchedEffect doesn't run there); animate at runtime.
    val inspection = LocalInspectionMode.current
    var visible by remember { mutableStateOf(inspection) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 2 }
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🇮🇳", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.festival_independence_day),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// The date/region gate is off outside Aug 15-20 in India, so this preview is how to see the
// festival otherwise (animations are static in a preview — run it on a device to see them move).
@Preview(name = "Independence Day — Home accent")
@Composable
private fun IndependenceDayPreview() {
    LiteratureTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TricolorWordmark(
                text = "♠\uFE0F ♥\uFE0F ♦\uFE0F ♣\uFE0F",
                baseStyle = LocalTextStyle.current,
                fontSize = 48.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            TricolorWordmark(
                text = "Literature",
                baseStyle = MaterialTheme.typography.displayMedium
            )
            Spacer(Modifier.height(12.dp))
            IndependenceDayGreeting()
        }
    }
}
