package com.cards.game.literature.preferences

import android.content.Context
import com.cards.game.literature.model.currentTimeMillis

/**
 * Android used to no-op all four of these, so losing the process mid-game ended the game. The
 * navigation stack came back on the online board; the repository, a process-scoped singleton,
 * did not. The player got an empty table under a red "Disconnected" and no way back — while the
 * server had been holding their seat the whole time (it plays as a bot after two minutes and
 * hands back the moment they return). The snapshot was the only missing piece.
 */
actual object OnlineSessionBackup {

    private const val PREFS = "lit_prefs"
    private const val KEY_ROOM = "session_room"
    private const val KEY_PLAYER = "session_player"
    private const val KEY_TOKEN = "session_token"
    private const val KEY_TOUCHED_AT = "session_touched_at"

    /**
     * The server sweeps a room 30 minutes after its last activity, so a snapshot older than that
     * names a room that no longer exists. The web twin needs no ceiling — its sessionStorage dies
     * with the tab — but this file outlives the process and would otherwise try to rejoin a game
     * from last week.
     */
    private const val MAX_AGE_MS = 30 * 60 * 1000L

    /** touch() runs on every inbound server message. Granularity of seconds is plenty against a
     *  30-minute window, and this keeps a fast exchange from writing to disk dozens of times. */
    private const val TOUCH_THROTTLE_MS = 10_000L

    private var appContext: Context? = null
    private var lastTouchMs = 0L

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    actual fun save(roomCode: String, playerId: String, reconnectToken: String) {
        val now = currentTimeMillis()
        lastTouchMs = now
        prefs()?.edit()
            ?.putString(KEY_ROOM, roomCode)
            ?.putString(KEY_PLAYER, playerId)
            ?.putString(KEY_TOKEN, reconnectToken)
            ?.putLong(KEY_TOUCHED_AT, now)
            ?.apply()
    }

    actual fun touch() {
        val now = currentTimeMillis()
        if (now - lastTouchMs < TOUCH_THROTTLE_MS) return
        lastTouchMs = now
        prefs()?.edit()?.putLong(KEY_TOUCHED_AT, now)?.apply()
    }

    actual fun load(): OnlineSessionSnapshot? {
        val prefs = prefs() ?: return null
        val room = prefs.getString(KEY_ROOM, null) ?: return null
        val player = prefs.getString(KEY_PLAYER, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        if (currentTimeMillis() - prefs.getLong(KEY_TOUCHED_AT, 0L) > MAX_AGE_MS) {
            clear()
            return null
        }
        return OnlineSessionSnapshot(room, player, token)
    }

    actual fun clear() {
        lastTouchMs = 0L
        prefs()?.edit()
            ?.remove(KEY_ROOM)
            ?.remove(KEY_PLAYER)
            ?.remove(KEY_TOKEN)
            ?.remove(KEY_TOUCHED_AT)
            ?.apply()
    }
}
