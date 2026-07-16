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
 *  3 — Option C: client can render the pass-target picker and send
 *      [ClientMessage.SelectPassTarget]. The server only suspends a claim for
 *      selection when the claimer's session reports version >= 3; older
 *      claimers keep the deterministic auto-pass.
 *  4 — Game Variants: client reads [RoomState.variants] and
 *      [PlayerGameView.turnDeadlineMs] and can send
 *      [ClientMessage.UpdateRoomSettings] to configure the per-room turn timer.
 *      The timer is a room-wide server-enforced setting, so nothing is gated on
 *      the client version; older clients simply ignore the new fields.
 */
object Protocol {
    const val VERSION: Int = 4

    /**
     * Oldest client protocol version the server still accepts. Raising this
     * above 1 will reject outdated apps at connect time with an
     * "update required" close reason — only do that once the active userbase
     * has largely moved past the old versions.
     */
    const val MIN_SUPPORTED: Int = 1
}
