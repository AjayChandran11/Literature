package com.cards.game.literature.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import java.util.concurrent.TimeUnit

/**
 * 20 seconds, set on OkHttp's own builder — the only place that works here.
 *
 * OkHttp sends a ping and fails the WebSocket when no pong comes back within the same interval,
 * which is what makes a half-open socket observable: the server frozen, a NAT dropping the flow,
 * a peer that vanished without a FIN. Without it the board stays on screen, the local countdown
 * keeps running, and nothing ever arrives again.
 *
 * Chosen against the server's 15s ping and 45s timeout: quick enough to notice inside a turn,
 * infrequent enough to be irrelevant to battery and data.
 */
private const val PING_SECONDS = 20L

actual fun createAppHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            // TimeUnit overload, not the java.time one: that needs API 26 and minSdk is 24.
            pingInterval(PING_SECONDS, TimeUnit.SECONDS)
        }
    }
    install(WebSockets)
    install(HttpTimeout)
}
