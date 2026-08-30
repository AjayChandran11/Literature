package com.cards.game.literature.preferences

import kotlinx.browser.localStorage

actual object StatsPrefs {
    actual fun getStatsJson(): String? = localStorage.getItem("stats_json")
    actual fun setStatsJson(json: String) {
        localStorage.setItem("stats_json", json)
    }

    actual fun getHistoryJson(): String? = localStorage.getItem("history_json")
    actual fun setHistoryJson(json: String) {
        localStorage.setItem("history_json", json)
    }

    actual fun getLastRecordedGameId(): String? = localStorage.getItem("last_recorded_game")
    actual fun setLastRecordedGameId(id: String) {
        localStorage.setItem("last_recorded_game", id)
    }

    actual fun getAchievementsJson(): String? = localStorage.getItem("achievements_json")
    actual fun setAchievementsJson(json: String) {
        localStorage.setItem("achievements_json", json)
    }

    actual fun getPuzzleJson(): String? = localStorage.getItem("puzzle_json")
    actual fun setPuzzleJson(json: String) {
        localStorage.setItem("puzzle_json", json)
    }
}
