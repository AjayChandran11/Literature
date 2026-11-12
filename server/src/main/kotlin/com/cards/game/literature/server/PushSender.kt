package com.cards.game.literature.server

import com.cards.game.literature.protocol.PushMessage

/** One kind of push: how urgently FCM should treat it, and how long it stays worth delivering. */
enum class PushType(val wire: String, val highPriority: Boolean, val ttlSeconds: Long) {
    /** Useless once the turn clock has run out, so it does not outlive it. */
    YOUR_TURN(PushMessage.TYPE_YOUR_TURN, highPriority = true, ttlSeconds = 60),
    GAME_STARTED(PushMessage.TYPE_GAME_STARTED, highPriority = true, ttlSeconds = 10 * 60),
    PLAYER_JOINED(PushMessage.TYPE_PLAYER_JOINED, highPriority = false, ttlSeconds = 10 * 60),
    GAME_OVER(PushMessage.TYPE_GAME_OVER, highPriority = false, ttlSeconds = 60 * 60),
    REMATCH(PushMessage.TYPE_REMATCH, highPriority = true, ttlSeconds = 10 * 60),
}

/** Delivers a push to one phone. Fire-and-forget: a room never waits on the push service. */
interface PushSender {
    /**
     * [data] is the payload the app reads (see [PushMessage]); [onInvalidToken] runs when the
     * service says the token is dead, so the seat can forget it rather than keep trying.
     */
    fun send(token: String, type: PushType, data: Map<String, String>, onInvalidToken: () -> Unit = {})

    /** No push service configured: every send is a no-op. */
    object None : PushSender {
        override fun send(token: String, type: PushType, data: Map<String, String>, onInvalidToken: () -> Unit) {}
    }
}
