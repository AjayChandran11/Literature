package com.cards.game.literature.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

actual object Analytics {
    private var delegate: FirebaseAnalytics? = null

    /**
     * Android-only. Call once from Application.onCreate — by then Firebase's init provider has
     * normally created the default FirebaseApp. Guarded anyway: when a Firebase component fails to
     * resolve (see the Crashlytics note in MainActivity) this runs even earlier, in
     * Application.onCreate, where a throw kills the process before any Activity exists. A null
     * delegate simply no-ops every call below.
     */
    fun init(context: Context) {
        delegate = runCatching { FirebaseAnalytics.getInstance(context.applicationContext) }.getOrNull()
    }

    actual fun log(event: AnalyticsEvent) {
        val fa = delegate ?: return
        fa.logEvent(event.name, event.params.toBundle())
    }

    actual fun setUserId(id: String?) {
        delegate?.setUserId(id)
    }

    actual fun setEnabled(enabled: Boolean) {
        delegate?.setAnalyticsCollectionEnabled(enabled)
    }

    private fun Map<String, Any>.toBundle(): Bundle? {
        if (isEmpty()) return null
        return Bundle().apply {
            for ((key, value) in this@toBundle) {
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putLong(key, value.toLong())
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putDouble(key, value.toDouble())
                    // Booleans aren't a first-class Analytics param type; log as a readable string.
                    is Boolean -> putString(key, value.toString())
                    else -> putString(key, value.toString())
                }
            }
        }
    }
}
