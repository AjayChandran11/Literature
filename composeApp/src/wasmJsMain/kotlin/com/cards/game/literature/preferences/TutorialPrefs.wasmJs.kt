package com.cards.game.literature.preferences


actual object TutorialPrefs {
    actual fun isFirstGameCompleted(): Boolean =
        WebStorage.get("tutorial_done")?.toBoolean() ?: false

    actual fun markFirstGameCompleted() {
        WebStorage.set("tutorial_done", "true")
    }

    actual fun isOnlineGateDismissed(): Boolean =
        WebStorage.get("online_gate_dismissed")?.toBoolean() ?: false

    actual fun markOnlineGateDismissed() {
        WebStorage.set("online_gate_dismissed", "true")
    }

    actual fun isFirstGameDebriefShown(): Boolean =
        WebStorage.get("first_debrief_shown")?.toBoolean() ?: false

    actual fun markFirstGameDebriefShown() {
        WebStorage.set("first_debrief_shown", "true")
    }
}
