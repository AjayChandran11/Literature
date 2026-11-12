package com.cards.game.literature.notifications

import com.cards.game.literature.preferences.GamePrefs
import com.cards.game.literature.preferences.OnlineSessionBackup
import com.cards.game.literature.repository.OnlineGameRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.core.context.GlobalContext

/**
 * Receives the server's pushes (data-only messages, so they arrive here whether or not the app
 * is on screen) and Firebase's token rotations. Runs in the app process, after
 * [com.cards.game.literature.LiteratureApplication] has initialised the notifier and prefs, so
 * a push that starts the process from cold still has everything it needs.
 */
class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        PushTokens.set(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // The live repository knows the table while the process is alive; the saved session is
        // what a process restarted by this very push has. Neither means the push is not for us.
        val currentRoom = runCatching { GlobalContext.get().get<OnlineGameRepository>().roomCode }
            .getOrNull()?.takeIf { it.isNotEmpty() }
            ?: runCatching { OnlineSessionBackup.load()?.roomCode }.getOrNull()
        val cue = PushMessageHandler.cueFor(
            data = message.data,
            inForeground = AppLifecycleObserver.isAppInForeground.value,
            notificationsEnabled = runCatching { GamePrefs.isNotificationsEnabled() }.getOrDefault(true),
            currentRoomCode = currentRoom,
        ) ?: return
        PushMessageHandler.show(cue)
    }
}
