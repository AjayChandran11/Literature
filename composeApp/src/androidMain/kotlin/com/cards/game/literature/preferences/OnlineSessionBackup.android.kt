package com.cards.game.literature.preferences

actual object OnlineSessionBackup {
    actual fun save(roomCode: String, playerId: String, reconnectToken: String) {}
    actual fun touch() {}
    actual fun load(): OnlineSessionSnapshot? = null
    actual fun clear() {}
}
