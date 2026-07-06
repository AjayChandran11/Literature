package com.cards.game.literature.deeplink

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-lifetime holder for a room code that arrived via a deep link (App Link or
 * custom scheme). The platform entry point (e.g. Android MainActivity) parses the
 * incoming intent and calls [submit]; the UI (HomeScreen) observes [pendingRoomCode]
 * and surfaces an invite, then [consume]s it once acted upon.
 *
 * Kept as a singleton object — like [com.cards.game.literature.network.NetworkMonitor] —
 * so the platform layer can set it without a DI lookup.
 */
object DeepLinkHandler {
    // Room codes are 6 chars from the server's alphabet (A–Z minus I/O, 2–9). We accept
    // any 6-char alnum after uppercasing; the server validates the actual code on join.
    private val ROOM_CODE_REGEX = Regex("^[A-Z0-9]{6}$")

    private val _pendingRoomCode = MutableStateFlow<String?>(null)
    val pendingRoomCode: StateFlow<String?> = _pendingRoomCode.asStateFlow()

    /** A screen the app should jump straight to when launched from an external entry point. */
    enum class LaunchDestination { DAILY_PUZZLE }

    private val _pendingDestination = MutableStateFlow<LaunchDestination?>(null)
    val pendingDestination: StateFlow<LaunchDestination?> = _pendingDestination.asStateFlow()

    /**
     * Records that the app was launched with a request to land on a specific screen — e.g. the
     * daily-puzzle reminder notification. The navigation layer observes [pendingDestination] and
     * routes there once composed, then [consumeDestination]s it.
     */
    fun submitDestination(destination: LaunchDestination?) {
        if (destination == null) return
        _pendingDestination.value = destination
    }

    /** Clears the pending destination once the UI has routed the user to it. */
    fun consumeDestination() {
        _pendingDestination.value = null
    }

    /**
     * Records an incoming invite. Accepts either a raw 6-char code or a full deep-link
     * URI string (https App Link or `literature://` scheme); extracts and validates the
     * code, ignoring anything malformed.
     */
    fun submit(raw: String?) {
        val code = extractRoomCode(raw) ?: return
        _pendingRoomCode.value = code
    }

    /** Clears the pending invite once the UI has routed the user to it. */
    fun consume() {
        _pendingRoomCode.value = null
    }

    /**
     * Pulls a room code out of a raw string. Handles:
     *  - a bare code: `ABC123`
     *  - https App Link: `https://host/Literature/join.html?room=ABC123`
     *  - custom scheme:  `literature://join?room=ABC123` or `literature://join/ABC123`
     * Returns the uppercased code if it matches [ROOM_CODE_REGEX], else null.
     */
    fun extractRoomCode(raw: String?): String? {
        val input = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null

        // 1) `room=` query parameter, wherever it appears.
        val fromQuery = Regex("[?&]room=([^&#]+)", RegexOption.IGNORE_CASE)
            .find(input)
            ?.groupValues
            ?.get(1)
        // 2) Fallback: the last path segment (strips a trailing extension like .html).
        val lastSegment = input
            .substringBefore('?')
            .substringBefore('#')
            .trimEnd('/')
            .substringAfterLast('/')
            .substringBefore('.')

        val candidate = (fromQuery ?: lastSegment).trim().uppercase()
        return if (ROOM_CODE_REGEX.matches(candidate)) candidate else null
    }
}
