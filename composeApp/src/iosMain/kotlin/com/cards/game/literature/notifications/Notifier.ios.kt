package com.cards.game.literature.notifications

import platform.Foundation.NSError
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

actual object Notifier {
    private const val ID_YOUR_TURN = "lit_your_turn"
    private const val ID_GAME_STARTING = "lit_game_starting"
    private const val ID_GAME_OVER = "lit_game_over"

    private fun post(id: String, title: String, body: String, silent: Boolean = false) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            if (!silent) setSound(UNNotificationSound.defaultSound())
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = id,
            content = content,
            trigger = null
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request) { _: NSError? -> }
    }

    actual fun notifyYourTurn() {
        post(ID_YOUR_TURN, "Your turn", "Tap to play your move")
    }

    actual fun notifyGameStarting() {
        post(ID_GAME_STARTING, "Game is starting", "Your room is ready")
    }

    actual fun notifyGameOver(won: Boolean) {
        val body = if (won) "Your team won — tap to see results" else "Your team lost — tap to see results"
        post(ID_GAME_OVER, "Game over", body, silent = true)
    }

    actual fun clearYourTurn() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removeDeliveredNotificationsWithIdentifiers(listOf(ID_YOUR_TURN))
    }

    actual fun clearAll() {
        UNUserNotificationCenter.currentNotificationCenter()
            .removeDeliveredNotificationsWithIdentifiers(
                listOf(ID_YOUR_TURN, ID_GAME_STARTING, ID_GAME_OVER)
            )
    }

    fun requestAuthorization(onResult: (granted: Boolean) -> Unit) {
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted: Boolean, _: NSError? ->
            onResult(granted)
        }
    }
}
