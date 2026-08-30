package com.cards.game.literature.share

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIPasteboard
import platform.UIKit.UIApplication

actual object Sharer {
    actual fun shareText(text: String) {
        val controller = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        root?.presentViewController(controller, animated = true, completion = null)
    }

    // Image sharing is deferred to Phase 4 (iOS launch); share the caption text for now.
    actual fun shareImage(pngBytes: ByteArray, caption: String) {
        shareText(caption)
    }

    // WhatsApp-first sharing is deferred to the iOS launch (like image sharing above); until
    // then iOS callers fall back to the system share sheet via [shareText].
    actual fun isWhatsAppAvailable(): Boolean = false

    actual fun shareTextToWhatsApp(text: String): Boolean = false

    actual fun copyText(text: String): Boolean {
        UIPasteboard.generalPasteboard.string = text
        return true
    }
}
