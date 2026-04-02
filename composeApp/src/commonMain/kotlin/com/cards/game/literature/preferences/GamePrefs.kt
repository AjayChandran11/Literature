package com.cards.game.literature.preferences

expect object GamePrefs {
    fun isSoundEnabled(): Boolean
    fun setSoundEnabled(enabled: Boolean)
    fun isHapticsEnabled(): Boolean
    fun setHapticsEnabled(enabled: Boolean)
}
