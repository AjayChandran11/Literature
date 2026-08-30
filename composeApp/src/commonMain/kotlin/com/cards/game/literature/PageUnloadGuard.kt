package com.cards.game.literature

/**
 * Web-only guard against accidental data loss: while an unrecoverable game is in progress,
 * the browser asks for confirmation before refresh/close. No-op on Android/iOS, where the
 * OS back/quit flows already confirm.
 */
expect object PageUnloadGuard {
    fun setGameInProgress(inProgress: Boolean)
}
