package com.cards.game.literature.analytics

/**
 * Cross-platform analytics facade. Android forwards to Firebase Analytics; iOS is a no-op today
 * (the iOS target ships no Firebase), so call sites in commonMain can log freely without guards.
 */
expect object Analytics {
    /** Report a product event. Safe to call before init / on iOS — it silently does nothing. */
    fun log(event: AnalyticsEvent)

    /** Associate events with a stable, non-PII install id (or null to clear). */
    fun setUserId(id: String?)

    /** Consent toggle — turns all collection on/off (maps to setAnalyticsCollectionEnabled). */
    fun setEnabled(enabled: Boolean)
}
