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

    /** Stored [com.cards.game.literature.ui.theme.ThemeMode] name; "SYSTEM" when unset. */
    fun getThemeMode(): String
    fun setThemeMode(mode: String)
    fun isDynamicColorsEnabled(): Boolean
    fun setDynamicColorsEnabled(enabled: Boolean)

    /** Whether the player overrides the stock bot pacing; the slider only applies when true. */
    fun isBotSpeedCustomEnabled(): Boolean
    fun setBotSpeedCustomEnabled(enabled: Boolean)

    /** Offline bot thinking time in seconds; [BotPacing.DEFAULT_SECONDS] when unset. */
    fun getBotDelaySeconds(): Float
    fun setBotDelaySeconds(seconds: Float)
}
