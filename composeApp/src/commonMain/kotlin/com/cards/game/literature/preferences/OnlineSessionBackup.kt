package com.cards.game.literature.preferences

class OnlineSessionSnapshot(
    val roomCode: String,
    val playerId: String,
    val reconnectToken: String,
)

/**
 * Snapshot of the live online session, for platforms where the app can die mid-game in a
 * recoverable way — a browser tab refresh — so it can rejoin within the server's 2-minute
 * reconnect window. Android/iOS actuals are no-ops (process death ends the session there today).
 */
expect object OnlineSessionBackup {
    fun save(roomCode: String, playerId: String, reconnectToken: String)

    /** Bump the liveness timestamp; called on every server message so [load] can tell a
     *  just-refreshed tab from one restored hours later. */
    fun touch()

    /** The saved session, or null if absent or older than the server's reconnect window. */
    fun load(): OnlineSessionSnapshot?

    fun clear()
}
