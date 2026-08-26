package com.cards.game.literature

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.cards.game.literature.deeplink.DeepLinkHandler
import com.cards.game.literature.ui.common.ResumeCurtain
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
            Box {
                AppNavigation()
                // Web session resume: covers the boot-time navigation hops until the
                // reattached game state lands (or the failure UI shows underneath).
                val resumeInFlight by DeepLinkHandler.resumeInFlight.collectAsState()
                if (resumeInFlight) {
                    ResumeCurtain()
                }
            }
        }
    }
}
