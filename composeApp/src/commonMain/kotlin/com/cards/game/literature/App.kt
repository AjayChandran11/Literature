package com.cards.game.literature

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cards.game.literature.ui.navigation.AppNavigation
import com.cards.game.literature.ui.theme.LiteratureTheme

@Composable
fun App() {
    LiteratureTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation()
        }
    }
}
