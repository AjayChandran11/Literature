package com.cards.game.literature.preferences

import platform.Foundation.NSUserDefaults

actual object GamePrefs {
    actual fun isSoundEnabled(): Boolean =
        if (NSUserDefaults.standardUserDefaults.objectForKey("sound_enabled") == null) true
        else NSUserDefaults.standardUserDefaults.boolForKey("sound_enabled")

    actual fun setSoundEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = "sound_enabled")
    }

    actual fun isHapticsEnabled(): Boolean =
        if (NSUserDefaults.standardUserDefaults.objectForKey("haptics_enabled") == null) true
        else NSUserDefaults.standardUserDefaults.boolForKey("haptics_enabled")

    actual fun setHapticsEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = "haptics_enabled")
    }

    actual fun isNotificationsEnabled(): Boolean =
        if (NSUserDefaults.standardUserDefaults.objectForKey("notifications_enabled") == null) true
        else NSUserDefaults.standardUserDefaults.boolForKey("notifications_enabled")

    actual fun setNotificationsEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = "notifications_enabled")
    }

    actual fun isPuzzleReminderEnabled(): Boolean =
        if (NSUserDefaults.standardUserDefaults.objectForKey("puzzle_reminder_enabled") == null) true
        else NSUserDefaults.standardUserDefaults.boolForKey("puzzle_reminder_enabled")

    actual fun setPuzzleReminderEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = "puzzle_reminder_enabled")
    }

    actual fun hasRequestedNotificationPermission(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey("notif_perm_requested")

    actual fun setRequestedNotificationPermission(requested: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(requested, forKey = "notif_perm_requested")
    }

    actual fun getThemeMode(): String =
        NSUserDefaults.standardUserDefaults.stringForKey("theme_mode") ?: "SYSTEM"

    actual fun setThemeMode(mode: String) {
        NSUserDefaults.standardUserDefaults.setObject(mode, forKey = "theme_mode")
    }

    actual fun isDynamicColorsEnabled(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey("dynamic_colors")

    actual fun setDynamicColorsEnabled(enabled: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(enabled, forKey = "dynamic_colors")
    }
}
