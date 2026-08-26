package com.cards.game.literature.audio

import com.cards.game.literature.preferences.GamePrefs
import literature.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Browsers only allow programmatic playback after the page has user activation; every sound
// here follows a click/tap by construction, and catch() swallows the rare pre-activation
// rejection instead of surfacing an unhandled-promise error in the console.
private fun playUrl(url: String): Unit =
    js("new Audio(url).play().catch(function(){})")

@OptIn(ExperimentalResourceApi::class)
actual object SoundPlayer {
    actual fun play(event: SoundEvent) {
        if (!GamePrefs.isSoundEnabled()) return
        playUrl(Res.getUri("files/${event.resName}.ogg"))
    }

    actual fun release() {}
}
