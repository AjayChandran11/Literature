package com.cards.game.literature

// The returnValue dance is what makes browsers show the native "Leave site?" confirmation.
private fun setBeforeUnload(enabled: Boolean): Unit =
    js(
        "window.onbeforeunload = enabled ? function(e) { e.preventDefault(); e.returnValue = ''; return ''; } : null"
    )

actual object PageUnloadGuard {
    actual fun setGameInProgress(inProgress: Boolean) {
        setBeforeUnload(inProgress)
    }
}
