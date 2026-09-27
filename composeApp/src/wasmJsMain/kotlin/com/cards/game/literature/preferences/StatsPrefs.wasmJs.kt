package com.cards.game.literature.preferences


actual object StatsPrefs {
    actual fun getStatsJson(): String? = WebStorage.get("stats_json")
    actual fun setStatsJson(json: String) {
        WebStorage.set("stats_json", json)
    }

    actual fun getHistoryJson(): String? = WebStorage.get("history_json")
    actual fun setHistoryJson(json: String) {
        WebStorage.set("history_json", json)
    }

    actual fun getLastRecordedGameId(): String? = WebStorage.get("last_recorded_game")
    actual fun setLastRecordedGameId(id: String) {
        WebStorage.set("last_recorded_game", id)
    }

    actual fun getAchievementsJson(): String? = WebStorage.get("achievements_json")
    actual fun setAchievementsJson(json: String) {
        WebStorage.set("achievements_json", json)
    }

    actual fun getPuzzleJson(): String? = WebStorage.get("puzzle_json")
    actual fun setPuzzleJson(json: String) {
        WebStorage.set("puzzle_json", json)
    }
}
