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
    var intentionalLeave: Boolean = false,
    /** Protocol version the client reported at connect (1 = legacy, no report). */
    var protocolVersion: Int = 1,
    /** Secret issued in RoomCreated; required to Reconnect (protocol v2+). */
    val reconnectToken: String = ""
) {
    suspend fun send(message: ServerMessage) {
        val ws = session ?: return
        try {
            ws.send(Frame.Text(Json.encodeToString(message)))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Dead or closing socket. Never let one broken connection abort a
            // broadcast loop, bot turns, or disconnect bookkeeping — the
            // socket gets reaped by its own close/ping-timeout path.
        }
    }
}
