package com.cards.game.literature.notifications

/** No scheduled notifications on web — a closed tab cannot be woken without push infrastructure. */
actual object PuzzleReminderScheduler {
    actual fun schedule() {}
    actual fun cancel() {}
}
