package com.cards.game.literature

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.cards.game.literature.deeplink.DeepLinkHandler
import com.cards.game.literature.di.appModule
import com.cards.game.literature.network.NetworkMonitor
import com.cards.game.literature.notifications.AppLifecycleObserver
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.compose.KoinApplication

private fun removeSplash(): Unit = js("document.getElementById('splash')?.remove()")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    AppLifecycleObserver.init()
    NetworkMonitor.startMonitoring()
    // Invite links carry ?room=CODE — the common parser already understands this shape.
    DeepLinkHandler.submit(window.location.href)
    ComposeViewport(document.body!!) {
        KoinApplication(application = { modules(appModule) }) {
            App()
        }
    }
    removeSplash()
}
