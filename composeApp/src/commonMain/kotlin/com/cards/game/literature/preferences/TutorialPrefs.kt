package com.cards.game.literature.preferences

expect object TutorialPrefs {
    fun isFirstGameCompleted(): Boolean
    fun markFirstGameCompleted()
}
