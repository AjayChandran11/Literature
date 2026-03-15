package com.cards.game.literature.preferences

expect object OnboardingPrefs {
    fun isCompleted(): Boolean
    fun markCompleted()
}
