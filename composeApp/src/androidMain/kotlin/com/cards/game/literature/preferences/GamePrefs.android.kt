package com.cards.game.literature.preferences

import android.content.Context

actual object GamePrefs {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext?.getSharedPreferences("lit_prefs", Context.MODE_PRIVATE)

    actual fun isSoundEnabled(): Boolean = prefs()?.getBoolean("sound_enabled", true) ?: true
    actual fun setSoundEnabled(enabled: Boolean) {
        prefs()?.edit()?.putBoolean("sound_enabled", enabled)?.apply()
    }

    actual fun isHapticsEnabled(): Boolean = prefs()?.getBoolean("haptics_enabled", true) ?: true
    actual fun setHapticsEnabled(enabled: Boolean) {
        prefs()?.edit()?.putBoolean("haptics_enabled", enabled)?.apply()
    }

    actual fun isNotificationsEnabled(): Boolean = prefs()?.getBoolean("notifications_enabled", true) ?: true
    actual fun setNotificationsEnabled(enabled: Boolean) {
        prefs()?.edit()?.putBoolean("notifications_enabled", enabled)?.apply()
    }

    actual fun isPuzzleReminderEnabled(): Boolean = prefs()?.getBoolean("puzzle_reminder_enabled", true) ?: true
    actual fun setPuzzleReminderEnabled(enabled: Boolean) {
        prefs()?.edit()?.putBoolean("puzzle_reminder_enabled", enabled)?.apply()
    }

    actual fun hasRequestedNotificationPermission(): Boolean =
        prefs()?.getBoolean("notif_perm_requested", false) ?: false
    actual fun setRequestedNotificationPermission(requested: Boolean) {
        prefs()?.edit()?.putBoolean("notif_perm_requested", requested)?.apply()
    }

    actual fun getThemeMode(): String = prefs()?.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    actual fun setThemeMode(mode: String) {
        prefs()?.edit()?.putString("theme_mode", mode)?.apply()
    }

    actual fun isDynamicColorsEnabled(): Boolean = prefs()?.getBoolean("dynamic_colors", false) ?: false
    actual fun setDynamicColorsEnabled(enabled: Boolean) {
        prefs()?.edit()?.putBoolean("dynamic_colors", enabled)?.apply()
    }
}
