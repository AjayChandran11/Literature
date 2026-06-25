package com.cards.game.literature.preferences

expect object GamePrefs {
    fun isSoundEnabled(): Boolean
    fun setSoundEnabled(enabled: Boolean)
    fun isHapticsEnabled(): Boolean
    fun setHapticsEnabled(enabled: Boolean)
    fun isNotificationsEnabled(): Boolean
    fun setNotificationsEnabled(enabled: Boolean)
    fun isPuzzleReminderEnabled(): Boolean
    fun setPuzzleReminderEnabled(enabled: Boolean)
    fun hasRequestedNotificationPermission(): Boolean
    fun setRequestedNotificationPermission(requested: Boolean)
}
