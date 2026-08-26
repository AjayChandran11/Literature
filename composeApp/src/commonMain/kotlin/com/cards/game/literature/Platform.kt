package com.cards.game.literature

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/** True in the browser build — for hiding affordances that have no web story. */
fun isWebPlatform(): Boolean = getPlatform().name == "Web"