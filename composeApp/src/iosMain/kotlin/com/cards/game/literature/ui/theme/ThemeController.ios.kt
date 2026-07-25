package com.cards.game.literature.ui.theme

import androidx.compose.runtime.Composable

actual val isDynamicColorSupported: Boolean = false

@Composable
actual fun SystemBarsEffect(darkTheme: Boolean) {
    // iOS status bar styling follows the system; nothing to do here.
}
