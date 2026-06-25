package com.cards.game.literature.stats

import com.cards.game.literature.preferences.StatsPrefs
import com.cards.game.literature.puzzle.PuzzleKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Loads, updates, and persists the local Daily Claim Puzzle progress + streak.
 * Pure transitions live in [PuzzleProgress]; this object only orchestrates
 * persistence and exposes reactive state for the UI (mirrors [StatsStore]).
 */
object PuzzleStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    private val _progress by lazy { MutableStateFlow(load()) }
    val progress: StateFlow<PuzzleProgress> get() = _progress.asStateFlow()

    /** Today's progress with day-scoped fields normalized to [today] (display only; not persisted). */
    fun today(today: Long = currentEpochDay()): PuzzleProgress = _progress.value.forDay(today)

    /** Record one solve attempt for [today]; persists and returns the updated progress. */
    suspend fun recordAttempt(correct: Boolean, today: Long = currentEpochDay()): PuzzleProgress =
        mutex.withLock {
            val updated = _progress.value.recordAttempt(today, correct)
            _progress.value = updated
            StatsPrefs.setPuzzleJson(json.encodeToString(updated))
            updated
        }

    /** Remember that the player has seen the how-to-play explainer for [kind]. */
    suspend fun markHowToSeen(kind: PuzzleKind): Unit = mutex.withLock {
        val current = _progress.value
        if (current.hasSeenHowTo(kind)) return@withLock
        val updated = current.withHowToSeen(kind)
        _progress.value = updated
        StatsPrefs.setPuzzleJson(json.encodeToString(updated))
    }

    /** Remember we flashed the Home "puzzle ready" highlight for [today] (a once-per-day nudge). */
    suspend fun markReadyHintShown(today: Long = currentEpochDay()): Unit = mutex.withLock {
        if (_progress.value.readyHintShownDay == today) return@withLock
        val updated = _progress.value.copy(readyHintShownDay = today)
        _progress.value = updated
        StatsPrefs.setPuzzleJson(json.encodeToString(updated))
    }

    private fun load(): PuzzleProgress = runCatching {
        StatsPrefs.getPuzzleJson()?.let { json.decodeFromString<PuzzleProgress>(it) }
    }.getOrNull() ?: PuzzleProgress()
}
