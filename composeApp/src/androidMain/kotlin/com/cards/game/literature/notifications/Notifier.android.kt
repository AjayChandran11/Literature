package com.cards.game.literature.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cards.game.literature.R

actual object Notifier {
    private var appContext: Context? = null

    private const val CHANNEL_TURN = "lit_turn"
    private const val CHANNEL_GAME_STATE = "lit_game_state"
    private const val CHANNEL_RESULT = "lit_result"

    private const val ID_YOUR_TURN = 1001
    private const val ID_GAME_STARTING = 1002
    private const val ID_GAME_OVER = 1003

    const val EXTRA_FROM_NOTIFICATION = "from_notification"

    fun init(context: Context) {
        appContext = context.applicationContext
        ensureChannels()
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ctx = appContext ?: return
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TURN,
                "Your turn",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts you when it's your turn in an online game."
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_GAME_STATE,
                "Game starting",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts you when your online game is starting."
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RESULT,
                "Game result",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Quietly tells you when your online game has ended."
            }
        )
    }

    private fun canPostNotifications(): Boolean {
        val ctx = appContext ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun launchIntent(): PendingIntent? {
        val ctx = appContext ?: return null
        val intent = ctx.packageManager
            .getLaunchIntentForPackage(ctx.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_FROM_NOTIFICATION, true)
            } ?: return null
        return PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun post(id: Int, channelId: String, title: String, body: String) {
        if (!canPostNotifications()) return
        val ctx = appContext ?: return
        val notification = NotificationCompat.Builder(ctx, channelId)
            .setSmallIcon(R.mipmap.app_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(launchIntent())
            .build()
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }

    actual fun notifyYourTurn() {
        post(ID_YOUR_TURN, CHANNEL_TURN, "Your turn", "Tap to play your move")
    }

    actual fun notifyGameStarting() {
        post(ID_GAME_STARTING, CHANNEL_GAME_STATE, "Game is starting", "Your room is ready")
    }

    actual fun notifyGameOver(won: Boolean) {
        val body = if (won) "Your team won — tap to see results" else "Your team lost — tap to see results"
        post(ID_GAME_OVER, CHANNEL_RESULT, "Game over", body)
    }

    actual fun clearYourTurn() {
        val ctx = appContext ?: return
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(ID_YOUR_TURN)
    }

    actual fun clearAll() {
        val ctx = appContext ?: return
        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(ID_YOUR_TURN)
        manager.cancel(ID_GAME_STARTING)
        manager.cancel(ID_GAME_OVER)
    }
}
