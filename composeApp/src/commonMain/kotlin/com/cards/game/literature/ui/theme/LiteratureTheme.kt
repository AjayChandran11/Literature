package com.cards.game.literature.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Shared semantic tokens ────────────────────────────────────────────────
// These are the named colours used throughout the UI. Edit here to retheme.

// Greens
val DarkGreen = Color(0xFF1B5E20)
val FeltGreen = Color(0xFF2E7D32)
val LightGreen = Color(0xFF4CAF50)

// Reds
val CardRed = Color(0xFFD32F2F)

// Golds
val GoldAccent = Color(0xFFFFD54F)
val DarkGold = Color(0xFFF9A825)

// Dark-mode surface palette
val SurfaceDark = Color(0xFF1A1A2E)
val SurfaceVariantDark = Color(0xFF16213E)
val OnSurfaceLight = Color(0xFFE0E0E0)

// ─── Dark colour scheme ────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary = LightGreen,
    onPrimary = Color.White,
    primaryContainer = DarkGreen,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = DarkGold,
    onSecondaryContainer = Color.Black,
    tertiary = CardRed,
    onTertiary = Color.White,
    background = SurfaceDark,
    onBackground = OnSurfaceLight,
    surface = SurfaceVariantDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = Color(0xFF1E3A2F),
    onSurfaceVariant = Color(0xFFB0BEC5),
    error = Color(0xFFEF5350),
    onError = Color.White,
    outline = Color(0xFF4A6741),
)

// ─── Light colour scheme ───────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = FeltGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF002106),
    secondary = DarkGold,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFE082),
    onSecondaryContainer = Color(0xFF2C1900),
    tertiary = CardRed,
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF424242),
    error = Color(0xFFB00020),
    onError = Color.White,
    outline = Color(0xFF4CAF50),
)

// ─── Theme composable ──────────────────────────────────────────────────────
/**
 * Apply the Literature theme.
 *
 * @param darkTheme      Force dark or light mode. Defaults to the system setting.
 * @param dynamicColor   Use Material You wallpaper colours on Android 12+.
 *                       Falls back to the static scheme on older devices / iOS.
 */
@Composable
fun LiteratureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor -> rememberDynamicColorScheme(darkTheme) ?: if (darkTheme) DarkColorScheme else LightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
