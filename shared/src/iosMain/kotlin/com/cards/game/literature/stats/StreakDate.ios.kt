package com.cards.game.literature.stats

import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.Foundation.secondsFromGMTForDate

actual fun localUtcOffsetSeconds(epochMillis: Long): Int =
    NSTimeZone.localTimeZone.secondsFromGMTForDate(
        NSDate(timeIntervalSince1970 = epochMillis / 1000.0)
    ).toInt()
