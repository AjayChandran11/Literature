package com.cards.game.literature.server

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RateLimiterTest {

    @Test
    fun allowsConnectionsUpToWindowLimit() {
        val limiter = RateLimiter(maxConnectionsPerWindow = 3, windowMs = 60_000, maxConcurrentPerIp = 100)
        repeat(3) {
            assertTrue(limiter.tryAcquire("1.2.3.4"))
            limiter.release("1.2.3.4")
        }
        assertFalse(limiter.tryAcquire("1.2.3.4"), "4th connection in window should be blocked")
    }

    @Test
    fun concurrentLimitBlocksAndReleaseFreesSlot() {
        val limiter = RateLimiter(maxConnectionsPerWindow = 100, windowMs = 60_000, maxConcurrentPerIp = 2)
        assertTrue(limiter.tryAcquire("1.2.3.4"))
        assertTrue(limiter.tryAcquire("1.2.3.4"))
        assertFalse(limiter.tryAcquire("1.2.3.4"), "3rd concurrent connection should be blocked")

        limiter.release("1.2.3.4")
        assertTrue(limiter.tryAcquire("1.2.3.4"), "slot freed by release should be reusable")
    }

    @Test
    fun ipsAreLimitedIndependently() {
        val limiter = RateLimiter(maxConnectionsPerWindow = 1, windowMs = 60_000, maxConcurrentPerIp = 1)
        assertTrue(limiter.tryAcquire("1.1.1.1"))
        assertTrue(limiter.tryAcquire("2.2.2.2"), "second IP must not share the first IP's budget")
        assertFalse(limiter.tryAcquire("1.1.1.1"))
    }

    @Test
    fun releasingUnknownIpIsHarmless() {
        val limiter = RateLimiter()
        limiter.release("9.9.9.9") // must not throw or corrupt state
        assertTrue(limiter.tryAcquire("9.9.9.9"))
    }
}
