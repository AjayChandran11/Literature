package com.cards.game.literature

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

// Constant per process; cached because call sites include per-frame recomposition paths.
private val isWeb: Boolean by lazy { getPlatform().name == "Web" }

/** True in the browser build — for hiding affordances that have no web story. */
fun isWebPlatform(): Boolean = isWeb