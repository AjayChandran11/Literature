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
}
