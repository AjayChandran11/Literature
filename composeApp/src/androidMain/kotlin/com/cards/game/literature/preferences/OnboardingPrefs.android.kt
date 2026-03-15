package com.cards.game.literature.preferences

import android.content.Context

actual object OnboardingPrefs {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun isCompleted(): Boolean {
        val ctx = appContext ?: return false
        return ctx.getSharedPreferences("lit_prefs", Context.MODE_PRIVATE)
            .getBoolean("onboarding_done", false)
    }

    actual fun markCompleted() {
        val ctx = appContext ?: return
        ctx.getSharedPreferences("lit_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("onboarding_done", true).apply()
    }
}
