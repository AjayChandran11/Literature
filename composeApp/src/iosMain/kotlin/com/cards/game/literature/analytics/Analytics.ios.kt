package com.cards.game.literature.analytics

/**
 * The iOS target ships no Firebase SDK, so analytics is a no-op here. Kept as a real actual (rather
 * than an #ifdef at every call site) so commonMain can log unconditionally. If iOS Firebase is ever
 * added, swap these bodies for FIRAnalytics calls — no call site changes.
 */
actual object Analytics {
    actual fun log(event: AnalyticsEvent) {}
    actual fun setUserId(id: String?) {}
    actual fun setEnabled(enabled: Boolean) {}
}
