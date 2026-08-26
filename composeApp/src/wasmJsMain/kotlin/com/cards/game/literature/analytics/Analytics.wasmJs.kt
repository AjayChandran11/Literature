package com.cards.game.literature.analytics

// gtag exists only when index.html carries a GA4 snippet (see the commented block there);
// without it these are silent no-ops, so the facade stays safe to call unconditionally.
private fun gtagEvent(name: String, paramsJson: String): Unit =
    js("window.gtag && window.gtag('event', name, JSON.parse(paramsJson))")

private fun gtagConsent(granted: Boolean): Unit =
    js("window.gtag && window.gtag('consent', 'update', { analytics_storage: granted ? 'granted' : 'denied' })")

private fun Map<String, Any>.toJson(): String =
    entries.joinToString(separator = ",", prefix = "{", postfix = "}") { (key, value) ->
        val encoded = when (value) {
            is Boolean, is Number -> value.toString()
            else -> "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        }
        "\"$key\":$encoded"
    }

actual object Analytics {
    actual fun log(event: AnalyticsEvent) {
        gtagEvent(event.name, event.params.toJson())
    }

    actual fun setUserId(id: String?) {
        // GA4 user_id is set via gtag('config', ...) — wire when the measurement ID lands.
    }

    actual fun setEnabled(enabled: Boolean) {
        gtagConsent(enabled)
    }
}
