package com.cards.game.literature.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/** Platform hook run once beside the NavHost — web binds the controller to browser history. */
@Composable
expect fun PlatformNavigationEffects(navController: NavHostController)
