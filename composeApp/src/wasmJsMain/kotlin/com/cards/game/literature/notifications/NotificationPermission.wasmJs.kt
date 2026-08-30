package com.cards.game.literature.notifications

import androidx.compose.runtime.Composable

@Composable
actual fun RequestNotificationPermissionOnce() {
    // Web ships no notifications; never prompt.
}
