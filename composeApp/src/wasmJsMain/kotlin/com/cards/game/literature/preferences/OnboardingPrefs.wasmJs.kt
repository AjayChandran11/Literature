package com.cards.game.literature.preferences

import kotlinx.browser.localStorage

actual object OnboardingPrefs {
    actual fun isCompleted(): Boolean =
        localStorage.getItem("onboarding_done")?.toBoolean() ?: false

    actual fun markCompleted() {
        localStorage.setItem("onboarding_done", "true")
    }
}
