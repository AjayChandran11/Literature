package com.cards.game.literature.server

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Per-IP rate limiter for WebSocket connections.
 * Enforces both a connection rate (max connections per time window)
 * and a concurrent connection limit per IP.
 */
class RateLimiter(
    // 60/min (was 10): real players reconnect repeatedly on flaky mobile networks,
    // and friends playing together share one public IP (home WiFi / carrier NAT).
    // The old limit of 10 locked legitimate groups out of their own games.
    private val maxConnectionsPerWindow: Int = 60,
    private val windowMs: Long = 60_000L,
    // 15 (was 5): an 8-player game hosted from a single location needs 8 concurrent
    // connections, plus headroom for brief overlap during reconnects. 5 made it
    // impossible for co-located groups to all connect.
    private val maxConcurrentPerIp: Int = 15
) {
    private val log = LoggerFactory.getLogger("RateLimiter")

    private data class IpRecord(
        val connectionTimestamps: MutableList<Long> = mutableListOf(),
        var concurrentCount: Int = 0
    )

    private val records = ConcurrentHashMap<String, IpRecord>()

    /** Sweeps stale records on a timer. Started by [startCleanup], stopped by [stop]. */
    private var cleanupScope: CoroutineScope? = null

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
    /**
     * Starts the periodic sweep. [cleanup] existed from the beginning and nothing ever called
     * it: [release] only drops a record when its window happens to be empty at that moment, so
     * an address that connects once and leaves keeps an entry until something else evicts it.
     * On a long-lived process that is a slow leak of one entry per address, for ever.
     */
    fun startCleanup(intervalMs: Long = windowMs) {
        if (cleanupScope != null) return
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        cleanupScope = scope
        scope.launch {
            while (isActive) {
                delay(intervalMs)
                cleanup()
            }
        }
    }

    /** Stops the sweep. */
    fun stop() {
        cleanupScope?.cancel()
        cleanupScope = null
    }

    /** Test visibility: how many addresses are currently held. */
    internal val trackedAddressCount: Int get() = records.size

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
