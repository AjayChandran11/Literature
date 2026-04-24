package com.cards.game.literature.notifications

expect object Notifier {
    fun notifyYourTurn()
    fun notifyGameStarting()
    fun notifyGameOver(won: Boolean)
    fun clearYourTurn()
    fun clearAll()
}
