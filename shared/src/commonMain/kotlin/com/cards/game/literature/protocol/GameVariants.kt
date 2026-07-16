package com.cards.game.literature.protocol

import kotlinx.serialization.Serializable

/**
 * Optional house-rule settings for a room. Every field is defaulted to the classic
 * behavior, so older serialized payloads (and old clients that omit the object) decode
 * to the standard ruleset. Add future variants as more defaulted fields here.
 */
@Serializable
data class GameVariants(
    /**
     * Per-turn time limit in seconds; `null` = no timer (Off). Enforced server-side
     * for online games. Default 60 preserves the original fixed turn timer.
     */
    val turnTimerSeconds: Int? = DEFAULT_TIMER_SECONDS
) {
    /** Coerces an untrusted value (e.g. off the wire) back to a supported setting. */
    fun sanitized(): GameVariants =
        if (turnTimerSeconds == null || turnTimerSeconds in ALLOWED_TIMER_SECONDS) this
        else copy(turnTimerSeconds = DEFAULT_TIMER_SECONDS)

    companion object {
        const val DEFAULT_TIMER_SECONDS = 60

        /** The timer values the UI offers (besides Off). */
        val ALLOWED_TIMER_SECONDS = setOf(30, 60, 90)
    }
}
