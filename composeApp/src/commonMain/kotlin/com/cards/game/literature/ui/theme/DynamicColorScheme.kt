package com.cards.game.literature.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/**
 * Returns a dynamic (Material You / wallpaper-based) [ColorScheme] when the
 * platform supports it, or `null` to fall back to the static scheme.
 */
@Composable
expect fun rememberDynamicColorScheme(darkTheme: Boolean): ColorScheme?
