package com.cards.game.literature.preferences

import platform.Foundation.NSUserDefaults

actual object TutorialPrefs {
    actual fun isFirstGameCompleted(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey("tutorial_done")

    actual fun markFirstGameCompleted() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = "tutorial_done")
    }

    actual fun isOnlineGateDismissed(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey("online_gate_dismissed")

    actual fun markOnlineGateDismissed() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = "online_gate_dismissed")
    }

    actual fun isFirstGameDebriefShown(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey("first_debrief_shown")

    actual fun markFirstGameDebriefShown() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = "first_debrief_shown")
    }
}
