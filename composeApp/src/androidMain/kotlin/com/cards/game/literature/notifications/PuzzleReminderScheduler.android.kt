package com.cards.game.literature.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

actual object PuzzleReminderScheduler {
    private var appContext: Context? = null
    private const val REQUEST_CODE = 2001

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Arm an inexact daily alarm at the next [PuzzleReminder.HOUR_OF_DAY] local. Inexact keeps it
     * permission-free (no SCHEDULE_EXACT_ALARM) — a re-engagement nudge doesn't need minute precision.
     * Idempotent: FLAG_UPDATE_CURRENT replaces any prior alarm, so re-calling on launch just re-arms.
     */
    actual fun schedule() {
        val ctx = appContext ?: return
        val alarms = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarms.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            nextTriggerAtMillis(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent(ctx)
        )
    }

    actual fun cancel() {
        val ctx = appContext ?: return
        (ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(pendingIntent(ctx))
        Notifier.clearPuzzleReady()
    }

    private fun nextTriggerAtMillis(): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, PuzzleReminder.HOUR_OF_DAY)
            set(Calendar.MINUTE, PuzzleReminder.MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal.timeInMillis
    }

    private fun pendingIntent(ctx: Context): PendingIntent =
        PendingIntent.getBroadcast(
            ctx,
            REQUEST_CODE,
            Intent(ctx, PuzzleReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
