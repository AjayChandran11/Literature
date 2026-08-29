package com.cards.game.literature.analytics

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// gtag exists only when index.html carries a GA4 snippet (see the commented block there);
// without it these are silent no-ops, so the facade stays safe to call unconditionally.
private fun gtagEvent(name: String, paramsJson: String): Unit =
    js("window.gtag && window.gtag('event', name, JSON.parse(paramsJson))")

private fun gtagConsent(granted: Boolean): Unit =
    js("window.gtag && window.gtag('consent', 'update', { analytics_storage: granted ? 'granted' : 'denied' })")

// Same param typing as the Android facade's toBundle — GA4 custom dimensions are typed once,
// so web and Android must not fork a param into different types: numbers stay numeric,
// booleans (not a first-class Analytics type) and everything else go as readable strings.
private fun Map<String, Any>.toJson(): String =
    JsonObject(mapValues { (_, value) ->
        when (value) {
            is Int, is Long, is Double, is Float -> JsonPrimitive(value as Number)
            else -> JsonPrimitive(value.toString())
        }
    }).toString()

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
