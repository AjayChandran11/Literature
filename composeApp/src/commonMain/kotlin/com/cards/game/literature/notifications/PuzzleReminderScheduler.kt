package com.cards.game.literature.notifications

/** Shared knobs for the daily-puzzle reminder so both platforms agree on when it fires. */
object PuzzleReminder {
    /** Local hour-of-day (24h) the "today's puzzle is ready" nudge fires. A new puzzle is available
     *  from local midnight; evening lands the highest re-engagement without an early-morning ping. */
    const val HOUR_OF_DAY = 19
    const val MINUTE = 0
}

/**
 * Schedules a once-a-day local notification reminding the player that a fresh Daily Puzzle is
 * waiting. Unlike [Notifier] (reactive, fires on live game events), this is a *scheduled* nudge that
 * must fire even when the app is closed — Android via AlarmManager, iOS via a repeating calendar
 * trigger. Gated behind the dedicated "Daily puzzle reminder" setting and the OS notification grant.
 */
expect object PuzzleReminderScheduler {
    /** (Re)arm the daily reminder. Idempotent — safe to call on every launch. */
    fun schedule()

    /** Cancel any pending reminder (and clear one already shown). */
    fun cancel()
}
