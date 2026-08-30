package com.cards.game.literature.ui.common

import androidx.compose.runtime.Composable

/**
 * Multiplatform back-press hook. Android intercepts the system back gesture/button;
 * web and iOS have no equivalent surface today, so their actuals are no-ops (on web,
 * browser back is the navigation stack's concern, not an in-screen interceptor's).
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
