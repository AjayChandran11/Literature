package com.cards.game.literature.notifications

import com.cards.game.literature.protocol.PushMessage

/** What a push asks the phone to show, once it has been read and found worth showing. */
sealed class PushCue {
    data object YourTurn : PushCue()
    data object GameStarted : PushCue()
    data class PlayerJoined(val name: String) : PushCue()
    data class GameOver(val won: Boolean) : PushCue()
    data object Rematch : PushCue()
}

/**
 * Reads a push's data and decides whether it deserves a notification.
 *
 * The server only pushes to a seat whose socket is gone, but a socket can be gone for a moment
 * while the app is open (a reconnect), so the app has the last word: nothing is shown while it
 * is on screen, when the player has turned notifications off, or for a table the player is known
 * to have left. When the app cannot tell which table it is at, the push is trusted.
 */
object PushMessageHandler {

    fun cueFor(
        data: Map<String, String>,
        inForeground: Boolean,
        notificationsEnabled: Boolean,
        currentRoomCode: String?,
    ): PushCue? {
        if (inForeground || !notificationsEnabled) return null
        val room = data[PushMessage.KEY_ROOM_CODE]
        if (room != null && currentRoomCode != null && !room.equals(currentRoomCode, ignoreCase = true)) return null
        return when (data[PushMessage.KEY_TYPE]) {
            PushMessage.TYPE_YOUR_TURN -> PushCue.YourTurn
            PushMessage.TYPE_GAME_STARTED -> PushCue.GameStarted
            PushMessage.TYPE_PLAYER_JOINED ->
                data[PushMessage.KEY_PLAYER_NAME]?.takeIf { it.isNotBlank() }?.let { PushCue.PlayerJoined(it) }
            PushMessage.TYPE_GAME_OVER -> when (data[PushMessage.KEY_RESULT]) {
                PushMessage.RESULT_WON -> PushCue.GameOver(won = true)
                PushMessage.RESULT_LOST -> PushCue.GameOver(won = false)
                else -> null
            }
            PushMessage.TYPE_REMATCH -> PushCue.Rematch
            else -> null
        }
    }

    fun show(cue: PushCue) {
        when (cue) {
            PushCue.YourTurn -> Notifier.notifyYourTurn()
            PushCue.GameStarted -> Notifier.notifyGameStarting()
            is PushCue.PlayerJoined -> Notifier.notifyPlayerJoined(cue.name)
            is PushCue.GameOver -> Notifier.notifyGameOver(cue.won)
            PushCue.Rematch -> Notifier.notifyRematch()
        }
    }
}
