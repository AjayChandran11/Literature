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
 *  4 — Quick Play and public tables ([ClientMessage.QuickPlay],
 *      [ClientMessage.CreateRoom.isPublic], [RoomState.autoStartInMs]) and the
 *      [com.cards.game.literature.model.GameEvent.PlayerJoined] event; older
 *      clients are sent PlayerReconnected in its place.
 *  5 — [ClientMessage.RegisterPush]: a client may hand the server a push
 *      token for its seat. The server pushes only to a seat whose socket is
 *      gone, and never gates anything on this version — the message is a
 *      hint, not a capability.
 */
object Protocol {
    const val VERSION: Int = 5

    /**
     * Oldest client protocol version the server still accepts. Set to 2 on
     * 2026-07-29 to hard-reject legacy v1 clients (which sent no `?v=` param and
     * could be session-hijacked via tokenless reconnect) at connect time with an
     * "update required" close reason — done after the rollout canary confirmed no
     * v1 traffic remained over ~2 weeks. Raise further only once the active
     * userbase has largely moved past the intervening versions.
     */
    const val MIN_SUPPORTED: Int = 2
}
