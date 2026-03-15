package com.cards.game.literature.preferences

import platform.Foundation.NSUserDefaults

actual object OnboardingPrefs {
    actual fun isCompleted(): Boolean =
        NSUserDefaults.standardUserDefaults.boolForKey("onboarding_done")

    actual fun markCompleted() {
        NSUserDefaults.standardUserDefaults.setBool(true, forKey = "onboarding_done")
    }
}
