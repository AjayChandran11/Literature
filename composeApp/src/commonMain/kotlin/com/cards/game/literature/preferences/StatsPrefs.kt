package com.cards.game.literature.preferences

/** Raw key-value persistence for player stats; JSON handling lives in StatsStore. */
expect object StatsPrefs {
    fun getStatsJson(): String?
    fun setStatsJson(json: String)
    fun getHistoryJson(): String?
    fun setHistoryJson(json: String)
    fun getLastRecordedGameId(): String?
    fun setLastRecordedGameId(id: String)
}
