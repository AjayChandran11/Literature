package com.cards.game.literature.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.cards.game.literature.preferences.GamePrefs

/** The player's theme preference. SYSTEM follows the OS light/dark setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Observable theme preference, backed by [GamePrefs]. Reading [mode] or
 * [dynamicColors] inside composition subscribes to changes, so flipping the
 * setting re-themes the whole app instantly — no restart. State is loaded
 * lazily on first read (GamePrefs is initialised in the Application entry
 * points before any composition happens).
 */
object ThemeController {
    private val modeState by lazy {
        mutableStateOf(
            runCatching { ThemeMode.valueOf(GamePrefs.getThemeMode()) }.getOrDefault(ThemeMode.SYSTEM)
        )
    }
    private val dynamicState by lazy { mutableStateOf(GamePrefs.isDynamicColorsEnabled()) }

    var mode: ThemeMode
        get() = modeState.value
        set(value) {
            modeState.value = value
            GamePrefs.setThemeMode(value.name)
        }

    var dynamicColors: Boolean
        get() = dynamicState.value
        set(value) {
            dynamicState.value = value
            GamePrefs.setDynamicColorsEnabled(value)
        }
}

/** Whether Material You wallpaper colours are available on this device (Android 12+). */
expect val isDynamicColorSupported: Boolean

/**
 * Keeps the system status/navigation bar icon contrast in sync with the
 * IN-APP theme. Without this, forcing light mode while the OS is dark leaves
 * light status-bar icons on a light background. No-op on iOS.
 */
@Composable
expect fun SystemBarsEffect(darkTheme: Boolean)
