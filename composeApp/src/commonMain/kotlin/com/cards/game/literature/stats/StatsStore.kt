package com.cards.game.literature.stats

import com.cards.game.literature.model.currentTimeMillis
import com.cards.game.literature.preferences.StatsPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Result of recording one finished game. */
data class GameRecordResult(
    val updatedStats: PlayerStats,
    val newlyUnlocked: List<Achievement>
)

/** Achievements unlocked by a specific game, awaiting their result-screen celebration. */
data class PendingCelebration(
    val gameId: String,
    val achievements: List<Achievement>
)

/**
 * Loads, updates, and persists the local player's stats, match history,
 * and achievement unlocks. Pure aggregation/evaluation logic lives in
 * [PlayerStats.applying] and [AchievementEvaluator]; this object only
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

    /** Achievement name -> unlock timestamp (millis). String keys so a
     *  removed enum entry can never break decoding of old data. */
    private val _achievements by lazy { MutableStateFlow(loadAchievements()) }
    val achievements: StateFlow<Map<String, Long>> get() = _achievements.asStateFlow()

    /**
     * Achievements unlocked by the most recently recorded game, awaiting
     * their celebration on the result screen. A flow (not a return value)
     * because the recording GameViewModel is popped off the back stack
     * before the result screen's ViewModel exists.
     */
    private val _pendingCelebration = MutableStateFlow<PendingCelebration?>(null)
    val pendingCelebration: StateFlow<PendingCelebration?> get() = _pendingCelebration.asStateFlow()

    /**
     * Folds a finished game into stats/history and evaluates achievements,
     * exactly once per [gameId] (guards against re-observation of the same
     * FINISHED state, e.g. after ViewModel recreation). Returns null if this
     * game was already recorded.
     */
    suspend fun recordGame(gameId: String, record: MatchRecord): GameRecordResult? = mutex.withLock {
        if (StatsPrefs.getLastRecordedGameId() == gameId) return null
        StatsPrefs.setLastRecordedGameId(gameId)

        val updatedStats = _stats.value.applying(record)
        val updatedHistory = (listOf(record) + _history.value).take(HISTORY_LIMIT)

        val unlocked = _achievements.value
        val newlyUnlocked = AchievementEvaluator.satisfiedBy(updatedStats, record)
            .filter { it.name !in unlocked }
            .sortedBy { it.ordinal }
        val updatedAchievements =
            if (newlyUnlocked.isEmpty()) unlocked
            else unlocked + newlyUnlocked.associate { it.name to record.timestamp }

        _stats.value = updatedStats
        _history.value = updatedHistory
        _achievements.value = updatedAchievements
        StatsPrefs.setStatsJson(json.encodeToString(updatedStats))
        StatsPrefs.setHistoryJson(json.encodeToString(updatedHistory))
        if (newlyUnlocked.isNotEmpty()) {
            StatsPrefs.setAchievementsJson(json.encodeToString(updatedAchievements))
        }
        // Scoped to gameId and set unconditionally (even when empty) so a stale celebration
        // can't leak onto a later game's result screen, and a result-screen recreation
        // re-reads the matching payload instead of losing it.
        _pendingCelebration.value = PendingCelebration(gameId, newlyUnlocked)
        GameRecordResult(updatedStats, newlyUnlocked)
    }

    /**
     * Unlock any Daily Puzzle achievements newly satisfied by [progress] (the day-scoped progress
     * right after a solve), persisting into the SAME achievements map the gameplay ones use.
     * Idempotent — already-unlocked entries are skipped. Returns the newly-unlocked list (sorted by
     * ordinal) for the puzzle result screen to celebrate.
     */
    suspend fun recordPuzzleAchievements(progress: PuzzleProgress): List<Achievement> = mutex.withLock {
        val unlocked = _achievements.value
        val newlyUnlocked = PuzzleAchievementEvaluator.satisfiedBy(progress)
            .filter { it.name !in unlocked }
            .sortedBy { it.ordinal }
        if (newlyUnlocked.isNotEmpty()) {
            val now = currentTimeMillis()
            val updated = unlocked + newlyUnlocked.associate { it.name to now }
            _achievements.value = updated
            StatsPrefs.setAchievementsJson(json.encodeToString(updated))
        }
        newlyUnlocked
    }

    private fun loadStats(): PlayerStats = runCatching {
        StatsPrefs.getStatsJson()?.let { json.decodeFromString<PlayerStats>(it) }
    }.getOrNull() ?: PlayerStats()

    private fun loadHistory(): List<MatchRecord> = runCatching {
        StatsPrefs.getHistoryJson()?.let { json.decodeFromString<List<MatchRecord>>(it) }
    }.getOrNull() ?: emptyList()

    private fun loadAchievements(): Map<String, Long> = runCatching {
        StatsPrefs.getAchievementsJson()?.let { json.decodeFromString<Map<String, Long>>(it) }
    }.getOrNull() ?: emptyMap()
}
