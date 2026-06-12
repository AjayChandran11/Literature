package com.cards.game.literature.preferences

import android.content.Context

actual object StatsPrefs {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun prefs() = appContext?.getSharedPreferences("lit_stats", Context.MODE_PRIVATE)

    actual fun getStatsJson(): String? = prefs()?.getString("stats_json", null)
    actual fun setStatsJson(json: String) {
        prefs()?.edit()?.putString("stats_json", json)?.apply()
    }

    actual fun getHistoryJson(): String? = prefs()?.getString("history_json", null)
    actual fun setHistoryJson(json: String) {
        prefs()?.edit()?.putString("history_json", json)?.apply()
    }

    actual fun getLastRecordedGameId(): String? = prefs()?.getString("last_recorded_game", null)
    actual fun setLastRecordedGameId(id: String) {
        prefs()?.edit()?.putString("last_recorded_game", id)?.apply()
    }

    actual fun getAchievementsJson(): String? = prefs()?.getString("achievements_json", null)
    actual fun setAchievementsJson(json: String) {
        prefs()?.edit()?.putString("achievements_json", json)?.apply()
    }
}
