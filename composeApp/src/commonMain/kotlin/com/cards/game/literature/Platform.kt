package com.cards.game.literature

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

// Constant per process; cached because call sites include per-frame recomposition paths.
private val isWeb: Boolean by lazy { getPlatform().name == "Web" }

/** True in the browser build — for hiding affordances that have no web story. */
fun isWebPlatform(): Boolean = isWeb

/**
 * Rethrows platform-fatal errors (JVM/Native OOM etc.) so they crash and reach Crashlytics
 * instead of being absorbed by broad `catch (Throwable)` blocks. MUST be a no-op on web:
 * JS-interop libraries use kotlin.Error for ordinary failures — Ktor's wasm engine wraps
 * every fetch rejection in `Error("Fail to fetch", ...)` — so a blanket Error rethrow there
 * turns an expected network failure into a coroutine-killing crash.
 */
expect fun rethrowIfPlatformFatal(e: Throwable)