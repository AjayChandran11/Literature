package com.cards.game.literature.ui.theme

import androidx.compose.runtime.Composable

actual val isDynamicColorSupported: Boolean = false

@Composable
actual fun SystemBarsEffect(darkTheme: Boolean) {
    // No system bars in a browser tab.
}
