package com.cards.game.literature.preferences

expect object TutorialPrefs {
    fun isFirstGameCompleted(): Boolean
    fun markFirstGameCompleted()

    /** True once the player has chosen to go online past the first-timer gate.
     *  Separate from [isFirstGameCompleted] so skipping the gate suppresses the
     *  nag without disarming the offline in-game tutorial. */
    fun isOnlineGateDismissed(): Boolean
    fun markOnlineGateDismissed()

    /** True once the one-time first-game debrief has shown on the result screen. */
    fun isFirstGameDebriefShown(): Boolean
    fun markFirstGameDebriefShown()
}
