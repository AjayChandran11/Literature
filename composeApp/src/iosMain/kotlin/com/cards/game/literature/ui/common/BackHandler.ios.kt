package com.cards.game.literature.ui.common

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button; edge-swipe navigation is not wired today.
}
