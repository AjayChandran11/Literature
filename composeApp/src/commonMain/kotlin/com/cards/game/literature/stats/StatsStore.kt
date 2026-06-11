package com.cards.game.literature.stats

import com.cards.game.literature.preferences.StatsPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Loads, updates, and persists the local player's stats and match history.
 * Pure aggregation logic lives in [PlayerStats.applying]; this object only
 * orchestrates persistence and exposes reactive state for the UI.
 */
object StatsStore {
    private val json = Json { ignoreUnknownKeys = true }
    private const val HISTORY_LIMIT = 50
    private val mutex = Mutex()

    private val _stats by lazy { MutableStateFlow(loadStats()) }
    val stats: StateFlow<PlayerStats> get() = _stats.asStateFlow()

    private val _history by lazy { MutableStateFlow(loadHistory()) }
    val history: StateFlow<List<MatchRecord>> get() = _history.asStateFlow()

    /**
     * Folds a finished game into stats and history exactly once per [gameId]
     * (guards against re-observation of the same FINISHED state, e.g. after
     * ViewModel recreation). Returns true if the game was recorded.
     */
    suspend fun recordGame(gameId: String, record: MatchRecord): Boolean = mutex.withLock {
        if (StatsPrefs.getLastRecordedGameId() == gameId) return false
        StatsPrefs.setLastRecordedGameId(gameId)

        val updatedStats = _stats.value.applying(record)
        val updatedHistory = (listOf(record) + _history.value).take(HISTORY_LIMIT)

        _stats.value = updatedStats
        _history.value = updatedHistory
        StatsPrefs.setStatsJson(json.encodeToString(updatedStats))
        StatsPrefs.setHistoryJson(json.encodeToString(updatedHistory))
        true
    }

    private fun loadStats(): PlayerStats = runCatching {
        StatsPrefs.getStatsJson()?.let { json.decodeFromString<PlayerStats>(it) }
    }.getOrNull() ?: PlayerStats()

    private fun loadHistory(): List<MatchRecord> = runCatching {
        StatsPrefs.getHistoryJson()?.let { json.decodeFromString<List<MatchRecord>>(it) }
    }.getOrNull() ?: emptyList()
}
