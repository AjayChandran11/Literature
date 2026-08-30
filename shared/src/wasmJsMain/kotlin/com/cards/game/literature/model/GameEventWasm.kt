package com.cards.game.literature.model

private fun jsDateNow(): Double = js("Date.now()")

actual fun currentTimeMillis(): Long = jsDateNow().toLong()
