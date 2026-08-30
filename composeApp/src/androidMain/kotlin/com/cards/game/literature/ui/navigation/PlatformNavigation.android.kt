package com.cards.game.literature.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
actual fun PlatformNavigationEffects(navController: NavHostController) {
    // System back is handled by the activity's dispatcher; nothing to bind.
}
