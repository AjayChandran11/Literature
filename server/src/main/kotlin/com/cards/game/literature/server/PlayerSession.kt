package com.cards.game.literature.server

import io.ktor.websocket.*
import com.cards.game.literature.protocol.ServerMessage
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class PlayerSession(
    val playerId: String,
    val playerName: String,
    var session: WebSocketSession?,
    var isConnected: Boolean = true,
    var lastSeen: Long = System.currentTimeMillis(),
    var disconnectDeadline: Long? = null,
    var intentionalLeave: Boolean = false
) {
    suspend fun send(message: ServerMessage) {
        val text = Json.encodeToString(message)
        session?.send(Frame.Text(text))
    }
}
