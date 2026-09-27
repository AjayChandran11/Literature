package com.cards.game.literature.preferences

import com.cards.game.literature.model.currentTimeMillis
import kotlinx.browser.window

// sessionStorage: survives reload/navigation within the tab, dies with the tab — the closest
// browser analogue of "the process is still alive".
private const val KEY_ROOM = "lit_session_room"
private const val KEY_PLAYER = "lit_session_player"
private const val KEY_TOKEN = "lit_session_token"
private const val KEY_TOUCHED_AT = "lit_session_touched_at"

actual object OnlineSessionBackup {
    init {
        // The server's reconnect window starts at DISCONNECT, but touch() otherwise only runs
        // on inbound server messages — an idle lobby tab (no traffic for 2 min) would wrongly
        // fail the staleness check on refresh. Stamp the clock as the page goes away instead:
        // pagehide is the exact moment the disconnect (and the server's window) begins.
        runCatching {
            window.addEventListener("pagehide", {
                if (WebStorage.getSession(KEY_ROOM) != null) touch()
            })
        }
    }

    actual fun save(roomCode: String, playerId: String, reconnectToken: String) {
        WebStorage.setSession(KEY_ROOM, roomCode)
        WebStorage.setSession(KEY_PLAYER, playerId)
        WebStorage.setSession(KEY_TOKEN, reconnectToken)
        touch()
    }

    actual fun touch() {
        WebStorage.setSession(KEY_TOUCHED_AT, currentTimeMillis().toString())
    }

    actual fun load(): OnlineSessionSnapshot? {
        val room = WebStorage.getSession(KEY_ROOM) ?: return null
        val player = WebStorage.getSession(KEY_PLAYER) ?: return null
        val token = WebStorage.getSession(KEY_TOKEN) ?: return null
        // No age check. There used to be a 2-minute one, on the theory that the server's
        // reconnect window had closed — but mid-game the server keeps the seat for the whole
        // match (it turns into a bot and hands back on return), so the client was giving up on
        // resumes that would have worked. The server decides; a rejection clears the snapshot.
        return OnlineSessionSnapshot(room, player, token)
    }

    actual fun clear() {
        WebStorage.removeSession(KEY_ROOM)
        WebStorage.removeSession(KEY_PLAYER)
        WebStorage.removeSession(KEY_TOKEN)
        WebStorage.removeSession(KEY_TOUCHED_AT)
    }
}
