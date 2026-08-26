package com.cards.game.literature.preferences

import com.cards.game.literature.model.currentTimeMillis
import kotlinx.browser.window

// sessionStorage: survives reload/navigation within the tab, dies with the tab — the closest
// browser analogue of "the process is still alive".
private const val KEY_ROOM = "lit_session_room"
private const val KEY_PLAYER = "lit_session_player"
private const val KEY_TOKEN = "lit_session_token"
private const val KEY_TOUCHED_AT = "lit_session_touched_at"

/** Server-side reconnect window is 2 minutes; don't attempt resumes that can only fail. */
private const val MAX_AGE_MS = 120_000L

actual object OnlineSessionBackup {
    actual fun save(roomCode: String, playerId: String, reconnectToken: String) {
        val s = window.sessionStorage
        s.setItem(KEY_ROOM, roomCode)
        s.setItem(KEY_PLAYER, playerId)
        s.setItem(KEY_TOKEN, reconnectToken)
        touch()
    }

    actual fun touch() {
        window.sessionStorage.setItem(KEY_TOUCHED_AT, currentTimeMillis().toString())
    }

    actual fun load(): OnlineSessionSnapshot? {
        val s = window.sessionStorage
        val room = s.getItem(KEY_ROOM) ?: return null
        val player = s.getItem(KEY_PLAYER) ?: return null
        val token = s.getItem(KEY_TOKEN) ?: return null
        val touchedAt = s.getItem(KEY_TOUCHED_AT)?.toLongOrNull() ?: return null
        if (currentTimeMillis() - touchedAt > MAX_AGE_MS) {
            clear()
            return null
        }
        return OnlineSessionSnapshot(room, player, token)
    }

    actual fun clear() {
        val s = window.sessionStorage
        s.removeItem(KEY_ROOM)
        s.removeItem(KEY_PLAYER)
        s.removeItem(KEY_TOKEN)
        s.removeItem(KEY_TOUCHED_AT)
    }
}
