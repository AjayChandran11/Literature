package com.cards.game.literature.preferences

import platform.Foundation.NSUserDefaults

actual object StatsPrefs {
    private val defaults get() = NSUserDefaults.standardUserDefaults

    actual fun getStatsJson(): String? = defaults.stringForKey("stats_json")
    actual fun setStatsJson(json: String) {
        defaults.setObject(json, forKey = "stats_json")
    }

    actual fun getHistoryJson(): String? = defaults.stringForKey("history_json")
    actual fun setHistoryJson(json: String) {
        defaults.setObject(json, forKey = "history_json")
    }

    actual fun getLastRecordedGameId(): String? = defaults.stringForKey("last_recorded_game")
    actual fun setLastRecordedGameId(id: String) {
        defaults.setObject(id, forKey = "last_recorded_game")
    }

    actual fun getAchievementsJson(): String? = defaults.stringForKey("achievements_json")
    actual fun setAchievementsJson(json: String) {
        defaults.setObject(json, forKey = "achievements_json")
    }

    actual fun getPuzzleJson(): String? = defaults.stringForKey("puzzle_json")
    actual fun setPuzzleJson(json: String) {
        defaults.setObject(json, forKey = "puzzle_json")
    }
}
