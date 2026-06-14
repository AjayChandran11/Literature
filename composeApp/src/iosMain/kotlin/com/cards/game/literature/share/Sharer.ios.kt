package com.cards.game.literature.share

import platform.UIKit.UIActivityViewController
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
}
