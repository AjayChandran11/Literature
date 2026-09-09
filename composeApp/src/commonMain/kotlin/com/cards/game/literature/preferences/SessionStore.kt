package com.cards.game.literature.preferences

/** The player's name. Backed by [GamePrefs] so it survives a cold start, not just the process. */
class SessionStore {
    var playerName: String
        get() = GamePrefs.getPlayerName()
        set(value) = GamePrefs.setPlayerName(value)
}
