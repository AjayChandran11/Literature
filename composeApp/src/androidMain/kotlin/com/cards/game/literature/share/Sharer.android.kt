package com.cards.game.literature.share

import android.content.Context
import android.content.Intent

actual object Sharer {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun shareText(text: String) {
        val ctx = appContext ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // Started from the application context, so the chooser needs its own task.
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ctx.startActivity(chooser)
        } catch (_: Exception) {
            // No app available to handle the share — fail silently.
        }
    }
}
