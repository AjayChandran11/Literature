package com.cards.game.literature.preferences

import android.content.Context

actual object TutorialPrefs {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun isFirstGameCompleted(): Boolean {
        val ctx = appContext ?: return false
        return ctx.getSharedPreferences("lit_prefs", Context.MODE_PRIVATE)
            .getBoolean("tutorial_done", false)
    }

    actual fun markFirstGameCompleted() {
        val ctx = appContext ?: return
        ctx.getSharedPreferences("lit_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("tutorial_done", true).apply()
    }
}
