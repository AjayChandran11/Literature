package com.cards.game.literature.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.cards.game.literature.preferences.GamePrefs

@Composable
actual fun RequestNotificationPermissionOnce() {
    LaunchedEffect(Unit) {
        if (GamePrefs.hasRequestedNotificationPermission()) return@LaunchedEffect
        Notifier.requestAuthorization { _ ->
            GamePrefs.setRequestedNotificationPermission(true)
        }
    }
}
