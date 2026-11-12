package com.cards.game.literature.notifications

import com.cards.game.literature.protocol.PushMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushMessageHandlerTest {

    private fun push(type: String, vararg extra: Pair<String, String>, room: String? = "ABCDEF") =
        buildMap {
            put(PushMessage.KEY_TYPE, type)
            if (room != null) put(PushMessage.KEY_ROOM_CODE, room)
            putAll(extra)
        }

    private fun cue(
        data: Map<String, String>,
        inForeground: Boolean = false,
        notificationsEnabled: Boolean = true,
        currentRoomCode: String? = "ABCDEF",
    ) = PushMessageHandler.cueFor(data, inForeground, notificationsEnabled, currentRoomCode)

    @Test
    fun eachKindOfPushBecomesItsCue() {
        assertEquals(PushCue.YourTurn, cue(push(PushMessage.TYPE_YOUR_TURN)))
        assertEquals(PushCue.GameStarted, cue(push(PushMessage.TYPE_GAME_STARTED)))
        assertEquals(PushCue.PlayerJoined("Ravi"), cue(push(PushMessage.TYPE_PLAYER_JOINED, PushMessage.KEY_PLAYER_NAME to "Ravi")))
        assertEquals(PushCue.GameOver(won = true), cue(push(PushMessage.TYPE_GAME_OVER, PushMessage.KEY_RESULT to PushMessage.RESULT_WON)))
        assertEquals(PushCue.GameOver(won = false), cue(push(PushMessage.TYPE_GAME_OVER, PushMessage.KEY_RESULT to PushMessage.RESULT_LOST)))
        assertEquals(PushCue.Rematch, cue(push(PushMessage.TYPE_REMATCH)))
    }

    @Test
    fun nothingIsShownWhileTheAppIsOnScreenOrNotificationsAreOff() {
        assertNull(cue(push(PushMessage.TYPE_YOUR_TURN), inForeground = true))
        assertNull(cue(push(PushMessage.TYPE_YOUR_TURN), notificationsEnabled = false))
    }

    @Test
    fun aPushForATableThePlayerIsKnownToHaveLeftIsDropped() {
        assertNull(cue(push(PushMessage.TYPE_YOUR_TURN), currentRoomCode = "ZZZZZZ"))
        assertEquals(PushCue.YourTurn, cue(push(PushMessage.TYPE_YOUR_TURN, room = "abcdef")), "room codes compare regardless of case")
        assertEquals(PushCue.YourTurn, cue(push(PushMessage.TYPE_YOUR_TURN), currentRoomCode = null), "no table known: the push is trusted")
        assertEquals(PushCue.YourTurn, cue(push(PushMessage.TYPE_YOUR_TURN, room = null)), "a push without a room is trusted too")
    }

    @Test
    fun aPushMissingWhatItsCueNeedsIsDropped() {
        assertNull(cue(push(PushMessage.TYPE_PLAYER_JOINED)))
        assertNull(cue(push(PushMessage.TYPE_PLAYER_JOINED, PushMessage.KEY_PLAYER_NAME to "  ")))
        assertNull(cue(push(PushMessage.TYPE_GAME_OVER)))
        assertNull(cue(push(PushMessage.TYPE_GAME_OVER, PushMessage.KEY_RESULT to "draw")))
        assertNull(cue(push("something_newer")))
        assertNull(cue(emptyMap()))
    }
}
