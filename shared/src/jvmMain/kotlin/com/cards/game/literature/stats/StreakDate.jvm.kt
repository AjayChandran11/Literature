package com.cards.game.literature.stats

import java.util.TimeZone

actual fun localUtcOffsetSeconds(epochMillis: Long): Int =
    TimeZone.getDefault().getOffset(epochMillis) / 1000
