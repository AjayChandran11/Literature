package com.cards.game.literature

import androidx.compose.ui.window.ComposeUIViewController
import com.cards.game.literature.notifications.AppLifecycleObserver

fun MainViewController() = ComposeUIViewController {
    AppLifecycleObserver.init()
    App()
}
