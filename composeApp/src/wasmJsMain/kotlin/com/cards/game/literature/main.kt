package com.cards.game.literature

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.cards.game.literature.deeplink.DeepLinkHandler
import com.cards.game.literature.di.appModule
import com.cards.game.literature.network.NetworkMonitor
import com.cards.game.literature.notifications.AppLifecycleObserver
import com.cards.game.literature.preferences.OnlineSessionBackup
import kotlinx.browser.document
import kotlinx.browser.window
import org.koin.compose.KoinApplication

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    AppLifecycleObserver.init()
    NetworkMonitor.startMonitoring()
    // A live session snapshot (tab refresh) beats any ?room= invite still in the URL:
    // resume the seat instead of re-triggering the join flow.
    val resume = OnlineSessionBackup.load()
    if (resume != null) {
        DeepLinkHandler.submitResume(
            DeepLinkHandler.PendingResume(resume.roomCode, resume.playerId, resume.reconnectToken)
        )
    } else {
        // Invite links carry ?room=CODE — the common parser already understands this shape.
        DeepLinkHandler.submit(window.location.href)
    }
    ComposeViewport(document.body!!) {
        // Splash stays until the emoji fallback font is registered (see WebFonts.kt).
        WithEmojiFallback {
            WebPageColumn {
                KoinApplication(application = { modules(appModule) }) {
                    App()
                }
            }
        }
    }
}
