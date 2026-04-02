package com.cards.game.literature.audio

expect object SoundPlayer {
    fun play(event: SoundEvent)
    fun release()
}
