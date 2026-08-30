package com.cards.game.literature

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

// Native fatals (OutOfMemoryError etc.) must crash, never be absorbed.
actual fun rethrowIfPlatformFatal(e: Throwable) {
    if (e is Error) throw e
}
