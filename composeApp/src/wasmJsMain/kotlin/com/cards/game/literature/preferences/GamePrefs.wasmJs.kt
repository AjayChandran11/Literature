package com.cards.game.literature.preferences

import kotlinx.browser.localStorage

actual object GamePrefs {
    actual fun isSoundEnabled(): Boolean =
        localStorage.getItem("sound_enabled")?.toBoolean() ?: true

    actual fun setSoundEnabled(enabled: Boolean) {
        localStorage.setItem("sound_enabled", enabled.toString())
    }

    actual fun isHapticsEnabled(): Boolean =
        localStorage.getItem("haptics_enabled")?.toBoolean() ?: true

    actual fun setHapticsEnabled(enabled: Boolean) {
        localStorage.setItem("haptics_enabled", enabled.toString())
    }

    actual fun isNotificationsEnabled(): Boolean =
        localStorage.getItem("notifications_enabled")?.toBoolean() ?: true

    actual fun setNotificationsEnabled(enabled: Boolean) {
        localStorage.setItem("notifications_enabled", enabled.toString())
    }

    actual fun isPuzzleReminderEnabled(): Boolean =
        localStorage.getItem("puzzle_reminder_enabled")?.toBoolean() ?: true

    actual fun setPuzzleReminderEnabled(enabled: Boolean) {
        localStorage.setItem("puzzle_reminder_enabled", enabled.toString())
    }

    actual fun hasRequestedNotificationPermission(): Boolean =
        localStorage.getItem("notif_perm_requested")?.toBoolean() ?: false

    actual fun setRequestedNotificationPermission(requested: Boolean) {
        localStorage.setItem("notif_perm_requested", requested.toString())
    }

    actual fun getThemeMode(): String =
        localStorage.getItem("theme_mode") ?: "SYSTEM"

    actual fun setThemeMode(mode: String) {
        localStorage.setItem("theme_mode", mode)
    }

    actual fun isDynamicColorsEnabled(): Boolean =
        localStorage.getItem("dynamic_colors")?.toBoolean() ?: false

    actual fun setDynamicColorsEnabled(enabled: Boolean) {
        localStorage.setItem("dynamic_colors", enabled.toString())
    }
}
