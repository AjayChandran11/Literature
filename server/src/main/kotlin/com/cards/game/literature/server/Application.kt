package com.cards.game.literature.server

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

private val log = LoggerFactory.getLogger("Literature")

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    log.info("Starting Literature server on port {}", port)
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        configureServer()
    }.start(wait = true)
}

fun Application.configureServer() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = 65536L // 64 KB — game messages are small
        masking = false
    }
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = false
            ignoreUnknownKeys = true
        })
    }

    val roomManager = RoomManager()
    val rateLimiter = RateLimiter()

    routing {
        gameWebSocket(roomManager, rateLimiter)
    }
}
