package com.cards.game.literature.audio

actual object SoundPlayer {
    actual fun play(event: SoundEvent) {
        // iOS implementation — add AVFoundation playback here when sound files are ready
    }

    actual fun release() {}
}
