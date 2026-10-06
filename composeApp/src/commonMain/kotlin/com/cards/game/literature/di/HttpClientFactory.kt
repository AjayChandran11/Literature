package com.cards.game.literature.di

import io.ktor.client.HttpClient

/**
 * The app's HTTP/WebSocket client.
 *
 * Per-platform because keeping a dead connection detectable is an ENGINE concern, and the
 * engines disagree about who owns it. Ktor's `WebSockets.pingInterval` is only honoured by
 * engines that implement the WebSocket protocol themselves; the OkHttp engine delegates to
 * OkHttp, which has its own ping setting and ignores Ktor's entirely (verified: the engine jar
 * contains no reference to it). Setting it in common code therefore did nothing on Android —
 * a frozen server went unnoticed for minutes.
 */
expect fun createAppHttpClient(): HttpClient
