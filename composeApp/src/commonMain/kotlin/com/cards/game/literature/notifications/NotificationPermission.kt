package com.cards.game.literature.notifications

import androidx.compose.runtime.Composable

/**
 * Requests notification permission exactly once per install, at the site where
 * it's placed. If the user has previously declined or granted, this is a no-op.
 */
@Composable
expect fun RequestNotificationPermissionOnce()
