package com.cards.game.literature.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets

/**
 * No ping here, deliberately: the Js engine hands WebSockets to the browser, whose API exposes
 * no ping/pong to script — the browser keeps the connection alive and surfaces a close event on
 * its own. There is nothing to configure, so a ping setting would be decoration.
 */
actual fun createAppHttpClient(): HttpClient = HttpClient {
    install(WebSockets)
    install(HttpTimeout)
}
