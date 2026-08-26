package com.cards.game.literature.ui.common

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Browser back is handled by history/navigation, not an in-screen interceptor.
}
