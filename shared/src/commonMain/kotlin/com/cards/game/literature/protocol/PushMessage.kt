package com.cards.game.literature.protocol

/**
 * The data payload of a push notification, as the server builds it and the app reads it.
 *
 * A push carries no words of its own: the app chooses the title and body from [KEY_TYPE], in its
 * own language, so a copy change never needs a server deploy and nothing stale can be shown. The
 * server only pushes to a seat whose socket is gone; the app still has the last word, and shows
 * nothing while it is on screen.
 */
object PushMessage {
    const val KEY_TYPE = "type"
    const val KEY_ROOM_CODE = "roomCode"
    /** [TYPE_PLAYER_JOINED]: who sat down. */
    const val KEY_PLAYER_NAME = "playerName"
    /** [TYPE_GAME_OVER]: [RESULT_WON] or [RESULT_LOST], from the recipient's side of the table. */
    const val KEY_RESULT = "result"

    /** The turn is theirs and they are not here to see it. */
    const val TYPE_YOUR_TURN = "your_turn"
    /** The table dealt while they were away; a bot holds their seat until they return. */
    const val TYPE_GAME_STARTED = "game_started"
    /** Someone sat down at their table while they were away. */
    const val TYPE_PLAYER_JOINED = "player_joined"
    const val TYPE_GAME_OVER = "game_over"
    /** The host dealt again after a finished game; their seat was kept. */
    const val TYPE_REMATCH = "rematch"

    const val RESULT_WON = "won"
    const val RESULT_LOST = "lost"
}
