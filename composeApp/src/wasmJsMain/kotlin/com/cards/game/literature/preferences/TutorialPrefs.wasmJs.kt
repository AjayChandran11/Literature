package com.cards.game.literature.preferences

import kotlinx.browser.localStorage

actual object TutorialPrefs {
    actual fun isFirstGameCompleted(): Boolean =
        localStorage.getItem("tutorial_done")?.toBoolean() ?: false

    actual fun markFirstGameCompleted() {
        localStorage.setItem("tutorial_done", "true")
    }

    actual fun isOnlineGateDismissed(): Boolean =
        localStorage.getItem("online_gate_dismissed")?.toBoolean() ?: false

    actual fun markOnlineGateDismissed() {
        localStorage.setItem("online_gate_dismissed", "true")
    }

    actual fun isFirstGameDebriefShown(): Boolean =
        localStorage.getItem("first_debrief_shown")?.toBoolean() ?: false

    actual fun markFirstGameDebriefShown() {
        localStorage.setItem("first_debrief_shown", "true")
    }
}
