package com.cards.game.literature

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

// JVM fatals (OOM, StackOverflowError, LinkageError) must crash to Crashlytics,
// never be absorbed by a broad Throwable catch.
actual fun rethrowIfPlatformFatal(e: Throwable) {
    if (e is Error) throw e
}
