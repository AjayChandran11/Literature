package com.cards.game.literature.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Contrast of the palette, asserted rather than eyeballed.
 *
 * Both theme bugs in the September audit were arithmetic, not taste: the light theme's `outline`
 * was the brand green, so disabled controls read as live and a draw badge matched a win; and
 * several greens sat near 2.5:1 on their own backgrounds. Nobody catches that by looking — but
 * WCAG's formula does, and it costs nothing to run on every build.
 *
 * 4.5:1 is the AA minimum for body text, 3:1 for large text and for the boundary of a control.
 */
class ThemeContrastTest {

    /** WCAG 2.1 relative luminance. */
    private fun Color.wcagLuminance(): Double {
        fun channel(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red) + 0.7152 * channel(green) + 0.0722 * channel(blue)
    }

    private fun contrast(fg: Color, bg: Color): Double {
        val a = fg.wcagLuminance()
        val b = bg.wcagLuminance()
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    /** Composites [fg] at [alpha] over [bg], the way a translucent chip renders. */
    private fun blend(fg: Color, alpha: Float, bg: Color) = Color(
        red = fg.red * alpha + bg.red * (1 - alpha),
        green = fg.green * alpha + bg.green * (1 - alpha),
        blue = fg.blue * alpha + bg.blue * (1 - alpha),
    )

    private fun assertReadable(name: String, fg: Color, bg: Color, minimum: Double) {
        val ratio = contrast(fg, bg)
        assertTrue(
            ratio >= minimum,
            "$name is ${(ratio * 100).toInt() / 100.0}:1, below the $minimum:1 minimum"
        )
    }

    private fun ColorScheme.checkTextPairs(theme: String) {
        assertReadable("$theme onBackground on background", onBackground, background, 4.5)
        assertReadable("$theme onSurface on surface", onSurface, surface, 4.5)
        assertReadable("$theme onSurfaceVariant on surfaceVariant", onSurfaceVariant, surfaceVariant, 4.5)
        // onPrimary and onError are asserted per-theme below: the dark pair is a deliberate
        // exception, see acceptedDarkThemeContrastGaps.
        assertReadable("$theme onSecondary on secondary", onSecondary, secondary, 4.5)
        assertReadable("$theme onPrimaryContainer on primaryContainer", onPrimaryContainer, primaryContainer, 4.5)
        assertReadable("$theme onSecondaryContainer on secondaryContainer", onSecondaryContainer, secondaryContainer, 4.5)
    }

    @Test
    fun lightThemeTextIsReadable() = LightColorScheme.checkTextPairs("light")

    @Test
    fun darkThemeTextIsReadable() = DarkColorScheme.checkTextPairs("dark")

    @Test
    fun lightThemeAccentLabelsAreReadable() {
        assertReadable("light onPrimary on primary", LightColorScheme.onPrimary, LightColorScheme.primary, 4.5)
        assertReadable("light onError on error", LightColorScheme.onError, LightColorScheme.error, 4.5)
    }

    @Test
    fun acceptedDarkThemeContrastGaps() {
        // Two pairs in the dark theme sit below the 4.5:1 body minimum:
        //
        //   onPrimary on primary = 2.78:1   white on LightGreen — every filled primary button
        //   onError   on error   = 3.49:1   white on #EF5350    — the Leave Room / Quit label
        //
        // Both were fixed once (deep-tone on-colours, 5.93:1 and 4.9:1) and the change was
        // REJECTED on look: the owner prefers the white labels. That is a legitimate call — this
        // is a card game, not a public service — so the numbers stand as an accepted exception
        // rather than a defect. Pinned so they cannot drift further without someone noticing.
        val primaryRatio = contrast(DarkColorScheme.onPrimary, DarkColorScheme.primary)
        val errorRatio = contrast(DarkColorScheme.onError, DarkColorScheme.error)
        assertTrue(primaryRatio >= 2.77, "dark primary label dropped below the accepted 2.78:1 (now $primaryRatio)")
        assertTrue(errorRatio >= 3.48, "dark error label dropped below the accepted 3.49:1 (now $errorRatio)")
    }

    @Test
    fun errorTextIsReadableWhereItSitsDirectlyOnASurface() {
        // `error` is also used as a plain foreground — "Leave room", the disconnect line in the
        // waiting room — where the pair is error-on-surface, not onError-on-error.
        assertReadable("light error on surface", LightColorScheme.error, LightColorScheme.surface, 4.5)
        assertReadable("dark error on surface", DarkColorScheme.error, DarkColorScheme.surface, 4.5)
        // On surfaceVariant `error` is the 10dp connection dot in the waiting room, not text.
        // WCAG 1.4.11 asks 3:1 of a non-text indicator, not the 4.5:1 it asks of prose.
        assertReadable("dark error dot on surfaceVariant", DarkColorScheme.error, DarkColorScheme.surfaceVariant, 3.0)
    }

    @Test
    fun outlineReadsAsAnOutlineNotAnAccent() {
        // The whole F-33 bug in one assertion: `outline` is what Material spends on hairlines and
        // on anything meant to look inactive. When it was #4CAF50 it was nearly the same colour
        // as `primary`, so a disabled button looked enabled.
        val distance = contrast(LightColorScheme.outline, LightColorScheme.primary)
        assertTrue(
            distance < 2.0,
            "light outline should be a neutral, not a second accent (vs primary: $distance)"
        )
        assertReadable("light outline on surface", LightColorScheme.outline, LightColorScheme.surface, 3.0)
    }

    @Test
    fun theTeamGreensAreLegibleOnTheirOwnGrounds() {
        // successGreen picks per theme precisely because LightGreen fails on a light ground.
        assertReadable("FeltGreen on light surface", FeltGreen, LightColorScheme.surface, 4.5)
        assertReadable("LightGreen on dark surface", LightGreen, DarkColorScheme.surface, 4.5)
    }

    @Test
    fun theStealMarkNeedsADifferentTintPerTheme() {
        // The result screen's half-suit chips are a 20% wash of the team colour over the
        // surface. On dark that is a deep chip and the gold bolt sings; on light it is a pale
        // one and the same gold vanishes. This pins the arithmetic so the two-branch tint in
        // ResultScreen.stolenMarkTint cannot be "simplified" back to one colour.
        val lightChip = blend(LightGreen, 0.20f, LightColorScheme.surface)
        val darkChip = blend(LightGreen, 0.20f, DarkColorScheme.surface)

        assertTrue(
            contrast(GoldAccent, darkChip) >= 3.0,
            "gold is the right mark on a dark chip (${contrast(GoldAccent, darkChip)})"
        )
        assertTrue(
            contrast(GoldAccent, lightChip) < 3.0,
            "and is unusable on a light one — this is why the tint branches"
        )
        assertReadable("steal mark on a light chip", LightColorScheme.onSurface, lightChip, 3.0)
    }

    @Test
    fun aWinAndALossAreNotTheSameColour() {
        // Outcome badges must be distinguishable from each other, not only from the background.
        assertTrue(
            contrast(LightGreen, CardRed) > 1.3,
            "win and loss badges must not read as the same colour"
        )
    }
}
