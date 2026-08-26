package com.cards.game.literature.analytics

/** Web ships no analytics yet (Firebase has no wasm SDK); GA4 via gtag interop is the planned actual. */
actual object Analytics {
    actual fun log(event: AnalyticsEvent) {}
    actual fun setUserId(id: String?) {}
    actual fun setEnabled(enabled: Boolean) {}
}
