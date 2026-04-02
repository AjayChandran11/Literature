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
}
