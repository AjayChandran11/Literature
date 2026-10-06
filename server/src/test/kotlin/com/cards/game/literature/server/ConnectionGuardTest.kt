package com.cards.game.literature.server

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Who the server thinks you are, and whether it will talk to you at all.
 *
 * Both answers used to be taken on trust: the rate limiter believed a header the caller wrote,
 * and the upgrade accepted any origin. Neither is load-bearing against a determined attacker —
 * this is a card game with no ambient credential — but both are nearly free to get right.
 */
class ConnectionGuardTest {

    // ─── who the client is (F-45) ────────────────────────────────────────────

    @Test
    fun theAddressIsTakenFromTheProxyNotFromTheCaller() {
        // Render appends the address it actually saw, so the RIGHT-most entry is the trusted
        // one. Everything left of it was supplied by whoever connected.
        assertEquals(
            "203.0.113.7",
            clientAddress(listOf("198.51.100.1, 203.0.113.7"), socketAddress = "10.0.0.1")
        )
    }

    @Test
    fun aSpoofedHeaderCannotChangeWhoTheLimiterSees() {
        // The attack this closes: send a different fake value on every connection and the rate
        // limiter counts each one as a brand-new machine, so the limit never applies. Reading
        // the right-most entry gives the same real address every time, whatever is prepended.
        val realAddress = "203.0.113.7"
        val seen = (1..5).map { attempt ->
            clientAddress(listOf("10.1.1.$attempt, $realAddress"), socketAddress = "10.0.0.1")
        }
        assertEquals(setOf(realAddress), seen.toSet(), "every attempt resolves to one address")
    }

    @Test
    fun aSingleEntryHeaderIsTheClientAddress() {
        assertEquals("203.0.113.7", clientAddress(listOf("203.0.113.7"), socketAddress = "10.0.0.1"))
    }

    @Test
    fun repeatedHeadersAreTreatedAsOneList() {
        // A header may legitimately arrive more than once; the last value overall still wins.
        assertEquals(
            "203.0.113.9",
            clientAddress(listOf("198.51.100.1", "198.51.100.2, 203.0.113.9"), socketAddress = "10.0.0.1")
        )
    }

    @Test
    fun withoutTheHeaderTheSocketAddressIsUsed() {
        // Direct connections and local development: unspoofable, because it is the socket.
        assertEquals("10.0.0.1", clientAddress(emptyList(), socketAddress = "10.0.0.1"))
        assertEquals("10.0.0.1", clientAddress(listOf("   "), socketAddress = "10.0.0.1"))
    }

    // ─── whether we will talk to them (F-87) ─────────────────────────────────

    private val allowed = setOf(
        "literature-game.pages.dev",
        "*.literature-game.pages.dev",
        "localhost",
        "127.0.0.1",
    )

    @Test
    fun nativeClientsSendNoOriginAndAreAllowed() {
        // Android and iOS send no Origin header at all. Rejecting a missing one would lock out
        // every phone — the opposite of the intent.
        assertTrue(isOriginAllowed(null, allowed))
        assertTrue(isOriginAllowed("", allowed))
    }

    @Test
    fun theDeployedWebClientIsAllowed() {
        assertTrue(isOriginAllowed("https://literature-game.pages.dev", allowed))
    }

    @Test
    fun cloudflarePreviewDeploymentsAreAllowed() {
        // Every deploy publishes a <hash>.literature-game.pages.dev alias as well.
        assertTrue(isOriginAllowed("https://57773968.literature-game.pages.dev", allowed))
    }

    @Test
    fun localDevelopmentIsAllowed() {
        assertTrue(isOriginAllowed("http://localhost:8000", allowed))
        assertTrue(isOriginAllowed("http://127.0.0.1:8000", allowed))
    }

    @Test
    fun someoneElsesSiteIsRejected() {
        assertFalse(isOriginAllowed("https://example.com", allowed))
        assertFalse(isOriginAllowed("https://evil.test", allowed))
    }

    @Test
    fun aLookalikeDomainIsRejected() {
        // The suffix match must not accept a host that merely ENDS with the allowed text.
        assertFalse(isOriginAllowed("https://literature-game.pages.dev.evil.test", allowed))
        assertFalse(isOriginAllowed("https://notliterature-game.pages.dev", allowed))
    }

    @Test
    fun anUnparseableOriginIsRejected() {
        // This one found a hole in the first version of the check: Ktor's Url() parser is
        // lenient and defaults a missing host to "localhost", so every malformed value below
        // was being ALLOWED. The header is now matched against the one shape a browser sends.
        assertFalse(isOriginAllowed("not a url", allowed))
        assertFalse(isOriginAllowed("null", allowed)) // what a sandboxed iframe sends
        assertFalse(isOriginAllowed("literature-game.pages.dev", allowed)) // no scheme
        assertFalse(isOriginAllowed("https://", allowed))
        assertFalse(isOriginAllowed("://localhost", allowed))
    }

    // ─── the limiter's own bookkeeping (F-66) ────────────────────────────────

    @Test
    fun staleAddressesAreSweptAway() = runBlocking {
        // cleanup() existed from the start and nothing called it. release() only drops a record
        // when its window happens to be empty at that moment, so an address that connects once
        // and leaves keeps an entry — one per address, for the life of the process.
        val limiter = RateLimiter(windowMs = 50)
        repeat(5) { assertTrue(limiter.tryAcquire("10.0.0.$it")) }
        repeat(5) { limiter.release("10.0.0.$it") }

        limiter.startCleanup(intervalMs = 20)
        try {
            withTimeout(2_000) {
                while (limiter.trackedAddressCount > 0) delay(20)
            }
        } finally {
            limiter.stop()
        }
        assertEquals(0, limiter.trackedAddressCount)
    }

    @Test
    fun anAddressStillConnectedIsNotSweptAway() = runBlocking {
        val limiter = RateLimiter(windowMs = 50)
        assertTrue(limiter.tryAcquire("10.0.0.1")) // still holding the connection

        limiter.startCleanup(intervalMs = 20)
        try {
            delay(200)
            assertEquals(1, limiter.trackedAddressCount, "a live connection keeps its record")
        } finally {
            limiter.stop()
        }
    }
}
