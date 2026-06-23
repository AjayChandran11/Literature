package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
enum class PuzzleFeedback { NONE, NEED_CARD, WRONG_HALF_SUIT, WRONG_CARD }

data class DailyPuzzleUiState(
    val loading: Boolean = true,
    val puzzle: DailyPuzzle? = null,
    val puzzleNumber: Int = 0,
    val streak: Int = 0,
    val status: PuzzleStatus = PuzzleStatus.NOT_STARTED,
    val attemptsUsed: Int = 0,
    val attemptsMax: Int = PuzzleProgress.MAX_ATTEMPTS,
    val stars: Int = 0,
    /** Step 1: the half-suit the player thinks their team can claim. */
    val selectedHalfSuit: HalfSuit? = null,
    /** Step 2: the card the player thinks the teammate is hiding. */
    val selectedCard: Card? = null,
    val feedback: PuzzleFeedback = PuzzleFeedback.NONE,
    /** True once the day is over (solved/failed) — the screen reveals the solution. */
    val revealed: Boolean = false,
    val howToSeen: Boolean = true
)

/**
 * Drives the Daily Claim Puzzle solve screen. Two taps: pick the claimable half-suit,
 * then the one card the teammate is hiding. Validates locally against the answer and
 * records the result (+ the separate puzzle streak) via [PuzzleStore].
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
            revealed = progress.status.isTerminal(),
            howToSeen = progress.howToSeen
        )
    }

    fun selectHalfSuit(halfSuit: HalfSuit) {
        if (terminal()) return
        _uiState.value = _uiState.value.copy(
            selectedHalfSuit = halfSuit, selectedCard = null, feedback = PuzzleFeedback.NONE
        )
    }

    /** Re-open the half-suit picker (step 1). */
    fun clearHalfSuit() {
        if (terminal()) return
        _uiState.value = _uiState.value.copy(
            selectedHalfSuit = null, selectedCard = null, feedback = PuzzleFeedback.NONE
        )
    }

    fun selectCard(card: Card) {
        if (terminal()) return
        _uiState.value = _uiState.value.copy(selectedCard = card, feedback = PuzzleFeedback.NONE)
    }

    fun submit() {
        val s = _uiState.value
        val puzzle = s.puzzle ?: return
        val halfSuit = s.selectedHalfSuit ?: return
        if (terminal()) return
        val card = s.selectedCard ?: run {
            _uiState.value = s.copy(feedback = PuzzleFeedback.NEED_CARD)
            return
        }

        val rightHalfSuit = halfSuit == puzzle.answer.halfSuit
        val correct = rightHalfSuit && card == puzzle.answer.hiddenCard

        viewModelScope.launch {
            val today = currentEpochDay()
            val progress = PuzzleStore.recordAttempt(correct, today)
            val feedback = when {
                correct -> PuzzleFeedback.NONE
                !rightHalfSuit -> PuzzleFeedback.WRONG_HALF_SUIT
                else -> PuzzleFeedback.WRONG_CARD
            }
            _uiState.value = _uiState.value.copy(
                status = progress.status,
                attemptsUsed = progress.attemptsUsed,
                stars = progress.stars,
                streak = progress.displayedStreak(today),
                feedback = feedback,
                revealed = progress.status.isTerminal()
            )
        }
    }

    fun markHowToSeen() {
        if (_uiState.value.howToSeen) return
        _uiState.value = _uiState.value.copy(howToSeen = true)
        viewModelScope.launch { PuzzleStore.markHowToSeen() }
    }

    private fun terminal(): Boolean = _uiState.value.status.isTerminal()
    private fun PuzzleStatus.isTerminal() = this == PuzzleStatus.SOLVED || this == PuzzleStatus.FAILED
}
