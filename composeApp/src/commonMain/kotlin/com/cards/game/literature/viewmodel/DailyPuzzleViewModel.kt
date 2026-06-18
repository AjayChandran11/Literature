package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.puzzle.DailyPuzzle
import com.cards.game.literature.repository.DailyPuzzleRepository
import com.cards.game.literature.stats.PuzzleProgress
import com.cards.game.literature.stats.PuzzleStatus
import com.cards.game.literature.stats.PuzzleStore
import com.cards.game.literature.stats.currentEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Outcome of a submit, mapped to localized text by the screen (the VM has no resources). */
enum class PuzzleFeedback { NONE, ASSIGN_ALL, WRONG_HALF_SUIT, WRONG_PLACEMENTS }

data class DailyPuzzleUiState(
    val loading: Boolean = true,
    val puzzle: DailyPuzzle? = null,
    val puzzleNumber: Int = 0,
    val streak: Int = 0,
    val status: PuzzleStatus = PuzzleStatus.NOT_STARTED,
    val attemptsUsed: Int = 0,
    val attemptsMax: Int = PuzzleProgress.MAX_ATTEMPTS,
    val stars: Int = 0,
    /** The half-suit the player is currently claiming. */
    val selectedHalfSuit: HalfSuit? = null,
    /** Target card -> chosen holder seat id. */
    val assignments: Map<Card, String> = emptyMap(),
    val feedback: PuzzleFeedback = PuzzleFeedback.NONE,
    val wrongCount: Int = 0,
    /** True once the day is over (solved/failed) — the screen reveals the solution. */
    val revealed: Boolean = false
)

/**
 * Drives the Daily Claim Puzzle solve screen: loads today's deterministic puzzle,
 * tracks the in-progress claim (selected half-suit + card→holder assignments), and
 * validates a submission locally against the answer, recording the result (and the
 * separate puzzle streak) via [PuzzleStore].
 */
class DailyPuzzleViewModel(
    private val repository: DailyPuzzleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DailyPuzzleUiState())
    val uiState: StateFlow<DailyPuzzleUiState> = _uiState.asStateFlow()

    init {
        val today = currentEpochDay()
        val progress = PuzzleStore.today(today)
        _uiState.value = DailyPuzzleUiState(
            loading = false,
            puzzle = repository.todaysPuzzle(today),
            puzzleNumber = repository.puzzleNumber(today),
            streak = progress.displayedStreak(today),
            status = progress.status,
            attemptsUsed = progress.attemptsUsed,
            stars = progress.stars,
            revealed = progress.status.isTerminal()
        )
    }

    fun selectHalfSuit(halfSuit: HalfSuit) {
        if (terminal()) return
        _uiState.value = _uiState.value.copy(
            selectedHalfSuit = halfSuit, assignments = emptyMap(), feedback = PuzzleFeedback.NONE
        )
    }

    fun assign(card: Card, holderId: String) {
        if (terminal()) return
        _uiState.value = _uiState.value.copy(
            assignments = _uiState.value.assignments + (card to holderId), feedback = PuzzleFeedback.NONE
        )
    }

    fun submit() {
        val s = _uiState.value
        val puzzle = s.puzzle ?: return
        val halfSuit = s.selectedHalfSuit ?: return
        if (terminal()) return

        val cards = DeckUtils.getAllCardsForHalfSuit(halfSuit)
        if (cards.any { it !in s.assignments }) {
            _uiState.value = s.copy(feedback = PuzzleFeedback.ASSIGN_ALL)
            return
        }
        val rightHalfSuit = halfSuit == puzzle.answer.halfSuit
        val correct = rightHalfSuit && cards.all { s.assignments[it] == puzzle.answer.holderOf(it) }

        viewModelScope.launch {
            val today = currentEpochDay()
            val progress = PuzzleStore.recordAttempt(correct, today)
            val feedback: PuzzleFeedback
            var wrongCount = 0
            when {
                correct -> feedback = PuzzleFeedback.NONE
                !rightHalfSuit -> feedback = PuzzleFeedback.WRONG_HALF_SUIT
                else -> {
                    feedback = PuzzleFeedback.WRONG_PLACEMENTS
                    wrongCount = cards.count { s.assignments[it] != puzzle.answer.holderOf(it) }
                }
            }
            _uiState.value = _uiState.value.copy(
                status = progress.status,
                attemptsUsed = progress.attemptsUsed,
                stars = progress.stars,
                streak = progress.displayedStreak(today),
                feedback = feedback,
                wrongCount = wrongCount,
                revealed = progress.status.isTerminal()
            )
        }
    }

    private fun terminal(): Boolean = _uiState.value.status.isTerminal()
    private fun PuzzleStatus.isTerminal() = this == PuzzleStatus.SOLVED || this == PuzzleStatus.FAILED
}
