package com.cards.game.literature

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cards.game.literature.ui.navigation.AppNavigation
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.ui.theme.SystemBarsEffect
import com.cards.game.literature.ui.theme.ThemeController
import com.cards.game.literature.ui.theme.ThemeMode

@Composable
fun App() {
    val darkTheme = when (ThemeController.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    SystemBarsEffect(darkTheme)
    LiteratureTheme(
        darkTheme = darkTheme,
        dynamicColor = ThemeController.dynamicColors
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation()
        }
    }
}
