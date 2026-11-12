package com.cards.game.literature.notifications

expect object Notifier {
    fun notifyYourTurn()
    fun notifyGameStarting()
    fun notifyGameOver(won: Boolean)
    /** Someone sat down at the player's table while they were away. */
    fun notifyPlayerJoined(name: String)
    /** The host dealt again after a finished game; the player's seat was kept. */
    fun notifyRematch()
    fun clearYourTurn()
    /** Clear all app notifications on foreground — including the daily-puzzle tray reminder. Once
     *  the app is open the reminder has done its job (get the player back in); the in-app "!" badge
     *  on the Home Daily-Puzzle tile takes over from there. */
    fun clearAll()
}
