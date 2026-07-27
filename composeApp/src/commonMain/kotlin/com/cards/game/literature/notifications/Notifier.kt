package com.cards.game.literature.notifications

expect object Notifier {
    fun notifyYourTurn()
    fun notifyGameStarting()
    fun notifyGameOver(won: Boolean)
    fun clearYourTurn()
    /** Clear all app notifications on foreground — including the daily-puzzle tray reminder. Once
     *  the app is open the reminder has done its job (get the player back in); the in-app "!" badge
     *  on the Home Daily-Puzzle tile takes over from there. */
    fun clearAll()
}
