package com.cards.game.literature.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets

/** Darwin engine; the iOS target is not shipped today (see the known iOS compile gap). */
actual fun createAppHttpClient(): HttpClient = HttpClient {
    install(WebSockets)
    install(HttpTimeout)
}
