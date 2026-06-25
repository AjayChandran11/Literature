package com.cards.game.literature.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cards.game.literature.preferences.GamePrefs
import com.cards.game.literature.preferences.StatsPrefs
import com.cards.game.literature.stats.PuzzleStatus
import com.cards.game.literature.stats.PuzzleStore
import com.cards.game.literature.stats.currentEpochDay

/**
 * Fired by the daily AlarmManager alarm (see [PuzzleReminderScheduler]). Posts the "today's puzzle
 * is ready" nudge — but only if the player still wants it and hasn't already finished today's puzzle.
 *
 * The alarm can wake a cold process where [com.cards.game.literature.LiteratureApplication] ran but
 * the Activity never did, so the singletons are re-initialised defensively (all idempotent).
 */
class PuzzleReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GamePrefs.init(context)
        StatsPrefs.init(context)
        Notifier.init(context)

        if (!GamePrefs.isPuzzleReminderEnabled()) return
        // Don't nag if they've already solved (or used up) today's puzzle.
        val status = PuzzleStore.today(currentEpochDay()).status
        if (status == PuzzleStatus.SOLVED || status == PuzzleStatus.FAILED) return

        Notifier.notifyPuzzleReady()
    }
}
