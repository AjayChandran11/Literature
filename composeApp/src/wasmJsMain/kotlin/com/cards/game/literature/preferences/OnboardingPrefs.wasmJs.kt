package com.cards.game.literature.preferences


actual object OnboardingPrefs {
    actual fun isCompleted(): Boolean =
        WebStorage.get("onboarding_done")?.toBoolean() ?: false

    actual fun markCompleted() {
        WebStorage.set("onboarding_done", "true")
    }
}
