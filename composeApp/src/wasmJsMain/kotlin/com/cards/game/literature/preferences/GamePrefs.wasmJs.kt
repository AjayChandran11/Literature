package com.cards.game.literature.preferences


actual object GamePrefs {
    actual fun isSoundEnabled(): Boolean =
        WebStorage.get("sound_enabled")?.toBoolean() ?: true

    actual fun setSoundEnabled(enabled: Boolean) {
        WebStorage.set("sound_enabled", enabled.toString())
    }

    actual fun isHapticsEnabled(): Boolean =
        WebStorage.get("haptics_enabled")?.toBoolean() ?: true

    actual fun setHapticsEnabled(enabled: Boolean) {
        WebStorage.set("haptics_enabled", enabled.toString())
    }

    actual fun isNotificationsEnabled(): Boolean =
        WebStorage.get("notifications_enabled")?.toBoolean() ?: true

    actual fun setNotificationsEnabled(enabled: Boolean) {
        WebStorage.set("notifications_enabled", enabled.toString())
    }

    actual fun isPuzzleReminderEnabled(): Boolean =
        WebStorage.get("puzzle_reminder_enabled")?.toBoolean() ?: true

    actual fun setPuzzleReminderEnabled(enabled: Boolean) {
        WebStorage.set("puzzle_reminder_enabled", enabled.toString())
    }

    actual fun hasRequestedNotificationPermission(): Boolean =
        WebStorage.get("notif_perm_requested")?.toBoolean() ?: false

    actual fun setRequestedNotificationPermission(requested: Boolean) {
        WebStorage.set("notif_perm_requested", requested.toString())
    }

    actual fun getThemeMode(): String =
        WebStorage.get("theme_mode") ?: "SYSTEM"

    actual fun setThemeMode(mode: String) {
        WebStorage.set("theme_mode", mode)
    }

    actual fun isDynamicColorsEnabled(): Boolean =
        WebStorage.get("dynamic_colors")?.toBoolean() ?: false

    actual fun setDynamicColorsEnabled(enabled: Boolean) {
        WebStorage.set("dynamic_colors", enabled.toString())
    }

    actual fun isBotSpeedCustomEnabled(): Boolean =
        WebStorage.get("bot_speed_custom")?.toBoolean() ?: false

    actual fun setBotSpeedCustomEnabled(enabled: Boolean) {
        WebStorage.set("bot_speed_custom", enabled.toString())
    }

    actual fun getBotDelaySeconds(): Float =
        WebStorage.get("bot_delay_secs")?.toFloatOrNull() ?: BotPacing.DEFAULT_SECONDS

    actual fun setBotDelaySeconds(seconds: Float) {
        WebStorage.set("bot_delay_secs", seconds.toString())
    }

    actual fun getPlayerName(): String = WebStorage.get("player_name") ?: ""

    actual fun setPlayerName(name: String) {
        WebStorage.set("player_name", name)
    }
}
