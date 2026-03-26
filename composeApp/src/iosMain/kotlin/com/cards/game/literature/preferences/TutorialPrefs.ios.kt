package com.cards.game.literature.preferences

import platform.Foundation.NSUserDefaults

actual object TutorialPrefs {
    actual fun isFirstGameCompleted(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey("tutorial_done")

    actual fun markFirstGameCompleted() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = "tutorial_done")
    }
}
