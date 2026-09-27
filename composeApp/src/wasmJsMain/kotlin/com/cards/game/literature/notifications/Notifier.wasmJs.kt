package com.cards.game.literature.notifications

import kotlinx.browser.document

/**
 * The web has no notification tray to post to (that needs the Notification API plus a service
 * worker), but it has the browser tab, which is exactly where a player who has switched away is
 * looking. A turn that comes up while the tab is hidden used to be completely silent, and with a
 * 60-second clock the player simply timed out. The title carries the cue instead.
 */
actual object Notifier {

    private const val ALERT_PREFIX = "● Your turn — "

    private val baseTitle: String by lazy {
        runCatching { document.title }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Literature"
    }

    private fun setTitle(title: String) {
        runCatching { document.title = title }
    }

    actual fun notifyYourTurn() = setTitle(ALERT_PREFIX + baseTitle)

    actual fun notifyGameStarting() {}

    actual fun notifyGameOver(won: Boolean) = clearYourTurn()

    actual fun clearYourTurn() = setTitle(baseTitle)

    actual fun clearAll() = clearYourTurn()
}
