package com.cards.game.literature.server

import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-IP rate limiter for WebSocket connections.
 * Enforces both a connection rate (max connections per time window)
 * and a concurrent connection limit per IP.
 */
class RateLimiter(
    private val maxConnectionsPerWindow: Int = 10,
    private val windowMs: Long = 60_000L,
    private val maxConcurrentPerIp: Int = 5
) {
    private val log = LoggerFactory.getLogger("RateLimiter")

    private data class IpRecord(
        val connectionTimestamps: MutableList<Long> = mutableListOf(),
        var concurrentCount: Int = 0
    )

    private val records = ConcurrentHashMap<String, IpRecord>()

    /**
     * Attempts to allow a new connection from the given IP.
     * Returns true if allowed, false if rate-limited.
     */
    @Synchronized
    fun tryAcquire(ip: String): Boolean {
        val now = System.currentTimeMillis()
        val record = records.getOrPut(ip) { IpRecord() }

        // Evict timestamps outside the current window
        record.connectionTimestamps.removeAll { now - it > windowMs }

        // Check connection rate
        if (record.connectionTimestamps.size >= maxConnectionsPerWindow) {
            log.warn("Rate limit exceeded for IP {}: {} connections in window", ip, record.connectionTimestamps.size)
            return false
        }

        // Check concurrent connections
        if (record.concurrentCount >= maxConcurrentPerIp) {
            log.warn("Concurrent connection limit exceeded for IP {}: {}", ip, record.concurrentCount)
            return false
        }

        record.connectionTimestamps.add(now)
        record.concurrentCount++
        return true
    }

    /**
     * Called when a connection from the given IP is closed.
     */
    @Synchronized
    fun release(ip: String) {
        val record = records[ip] ?: return
        record.concurrentCount = (record.concurrentCount - 1).coerceAtLeast(0)

        // Clean up empty records to avoid unbounded memory growth
        if (record.concurrentCount == 0 && record.connectionTimestamps.isEmpty()) {
            records.remove(ip)
        }
    }

    /**
     * Periodically clean up stale IP records with no active connections.
     */
    @Synchronized
    fun cleanup() {
        val now = System.currentTimeMillis()
        val staleIps = records.entries.filter { (_, record) ->
            record.concurrentCount == 0 &&
                record.connectionTimestamps.all { now - it > windowMs }
        }.map { it.key }
        staleIps.forEach { records.remove(it) }
    }
}
