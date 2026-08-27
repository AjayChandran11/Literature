package com.cards.game.literature.preferences

/** Pacing for offline bots' simulated thinking time. */
object BotPacing {
    /** The stock pacing — identical to what the server (and every release so far) uses.
     *  Applies whenever the player hasn't enabled a custom thinking time. */
    val PRODUCTION_DELAY: LongRange = 3_500L..4_000L

    const val MIN_SECONDS = 2f
    const val MAX_SECONDS = 7f
    const val DEFAULT_SECONDS = 4f

    /** Snap points every 0.5s → (7−2)/0.5 − 1 intermediate steps for the Material slider. */
    const val SLIDER_STEPS = 9

    /** The chosen value is the CENTER of a ±8% window — jitter keeps bots feeling organic. */
    fun delayRangeFor(seconds: Float): LongRange {
        val base = (seconds.coerceIn(MIN_SECONDS, MAX_SECONDS) * 1000).toLong()
        return (base * 92 / 100)..(base * 108 / 100)
    }
}
