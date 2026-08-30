package com.cards.game.literature.notifications

/** Web ships no notifications (needs the Notification API + a service worker to be useful). */
actual object Notifier {
    actual fun notifyYourTurn() {}
    actual fun notifyGameStarting() {}
    actual fun notifyGameOver(won: Boolean) {}
    actual fun clearYourTurn() {}
    actual fun clearAll() {}
}
