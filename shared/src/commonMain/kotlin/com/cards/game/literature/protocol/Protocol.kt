package com.cards.game.literature.protocol

/**
 * Protocol version contract between client and server.
 *
 * The client reports its version via the `v` query parameter on the WebSocket
 * URL (`/game?v=N`). Clients that don't send it (v1.0.3 and earlier) are
 * treated as version 1. The server reports its version back in
 * [ServerMessage.RoomCreated.protocolVersion].
 *
 * Version history:
 *  1 — implicit baseline (app v1.0.3 and earlier); no version reported.
 *  2 — version reported via query param; groundwork for reconnect tokens.
 */
object Protocol {
    const val VERSION: Int = 2

    /**
     * Oldest client protocol version the server still accepts. Raising this
     * above 1 will reject outdated apps at connect time with an
     * "update required" close reason — only do that once the active userbase
     * has largely moved past the old versions.
     */
    const val MIN_SUPPORTED: Int = 1
}
