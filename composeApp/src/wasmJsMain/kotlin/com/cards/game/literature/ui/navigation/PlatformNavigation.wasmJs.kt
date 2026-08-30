package com.cards.game.literature.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
actual fun PlatformNavigationEffects(navController: NavHostController) {
    // Deliberately NOT bound to browser history: restoring a hash like #game/... on refresh
    // deep-links into stateful routes and deals a brand-new game (offline) or composes an
    // empty board (online). A clean boot instead runs the session-resume flow, which is the
    // correct refresh behavior. Revisit with a per-route mapping when URL sync matters.
}
