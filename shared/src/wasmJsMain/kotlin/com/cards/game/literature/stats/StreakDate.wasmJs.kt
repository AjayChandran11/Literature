package com.cards.game.literature.stats

// getTimezoneOffset() is minutes WEST of UTC (IST → -330), so negate for an east-positive offset.
private fun jsTimezoneOffsetMinutes(epochMillis: Double): Double =
    js("new Date(epochMillis).getTimezoneOffset()")

actual fun localUtcOffsetSeconds(epochMillis: Long): Int =
    (-jsTimezoneOffsetMinutes(epochMillis.toDouble()) * 60.0).toInt()
