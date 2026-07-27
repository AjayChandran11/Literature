package com.cards.game.literature.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cards.game.literature.preferences.GamePrefs

/**
 * Re-arms the daily puzzle reminder after a device reboot. AlarmManager alarms don't survive a
 * reboot, and nothing else re-schedules until the next app launch — so a lapsed player who never
 * reopens the app (exactly the person the nudge is for) would silently stop getting it.
 *
 * Delivery is still guarded by [PuzzleReminderReceiver] (opt-out + already-solved), so re-arming
 * here is safe even if the player disables reminders before the alarm next fires.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        GamePrefs.init(context)
        PuzzleReminderScheduler.init(context)
        if (GamePrefs.isPuzzleReminderEnabled()) {
            PuzzleReminderScheduler.schedule()
        }
    }
}
