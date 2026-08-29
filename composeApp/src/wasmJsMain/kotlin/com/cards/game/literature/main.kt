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
    // A live session snapshot (tab refresh) beats the ?room= invite still in the URL —
    // unless the URL names a DIFFERENT room: that's an explicit new invite (e.g. a link
    // opened into a tab that inherited sessionStorage) and must win over the old seat.
    val resume = OnlineSessionBackup.load()
    val urlRoom = DeepLinkHandler.extractRoomCode(window.location.href)
    if (resume != null && (urlRoom == null || urlRoom == resume.roomCode)) {
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
