package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.puzzle.DailyPuzzle
import com.cards.game.literature.puzzle.HalfSuitClaim
import com.cards.game.literature.puzzle.LocateCard
import com.cards.game.literature.puzzle.WastedAsk
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
enum class PuzzleFeedback { NONE, NEED_CARD, WRONG_HALF_SUIT, WRONG_CARD, WRONG_SEAT }

data class DailyPuzzleUiState(
    val loading: Boolean = true,
    val puzzle: DailyPuzzle? = null,
    val puzzleNumber: Int = 0,
    val streak: Int = 0,
    val status: PuzzleStatus = PuzzleStatus.NOT_STARTED,
    val attemptsUsed: Int = 0,
    val attemptsMax: Int = PuzzleProgress.MAX_ATTEMPTS,
    val stars: Int = 0,
    /** CLAIM step 1: the half-suit the player thinks their team can claim. */
    val selectedHalfSuit: HalfSuit? = null,
    /** CLAIM step 2: the card the player thinks the teammate is hiding. */
    val selectedCard: Card? = null,
    /** LOCATE / WASTED_ASK: the seat the player tapped (one-tap answer). */
    val selectedSeatId: String? = null,
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
        val puzzle = repository.todaysPuzzle(today)
        _uiState.value = DailyPuzzleUiState(
            loading = false,
            puzzle = puzzle,
            puzzleNumber = repository.puzzleNumber(today),
            streak = progress.displayedStreak(today),
            status = progress.status,
            attemptsUsed = progress.attemptsUsed,
            stars = progress.stars,
            revealed = progress.status.isTerminal(),
            howToSeen = puzzle?.let { progress.hasSeenHowTo(it.kind) } ?: true
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

    /** One-tap answer for LOCATE / WASTED_ASK. */
    fun selectSeat(seatId: String) {
        if (terminal()) return
        _uiState.value = _uiState.value.copy(selectedSeatId = seatId, feedback = PuzzleFeedback.NONE)
    }

    fun submit() {
        val s = _uiState.value
        val puzzle = s.puzzle ?: return
        if (terminal()) return

        // Resolve correctness per kind. A null here means "no answer chosen yet" — the screen keeps
        // submit disabled until it's set, so this just no-ops defensively (except CLAIM's missing
        // card, which surfaces a hint).
        val correct: Boolean
        val wrongFeedback: PuzzleFeedback
        when (val answer = puzzle.answer) {
            is HalfSuitClaim -> {
                val halfSuit = s.selectedHalfSuit ?: return
                val card = s.selectedCard ?: run {
                    _uiState.value = s.copy(feedback = PuzzleFeedback.NEED_CARD); return
                }
                val rightHalfSuit = halfSuit == answer.halfSuit
                correct = rightHalfSuit && card == answer.hiddenCard
                wrongFeedback = if (!rightHalfSuit) PuzzleFeedback.WRONG_HALF_SUIT else PuzzleFeedback.WRONG_CARD
            }
            is LocateCard -> {
                val seat = s.selectedSeatId ?: return
                correct = seat == answer.seatId
                wrongFeedback = PuzzleFeedback.WRONG_SEAT
            }
            is WastedAsk -> {
                val seat = s.selectedSeatId ?: return
                correct = seat == answer.seatId
                wrongFeedback = PuzzleFeedback.WRONG_SEAT
            }
        }

        viewModelScope.launch {
            val today = currentEpochDay()
            val progress = PuzzleStore.recordAttempt(correct, today)
            _uiState.value = _uiState.value.copy(
                status = progress.status,
                attemptsUsed = progress.attemptsUsed,
                stars = progress.stars,
                streak = progress.displayedStreak(today),
                feedback = if (correct) PuzzleFeedback.NONE else wrongFeedback,
                revealed = progress.status.isTerminal()
            )
        }
    }

    fun markHowToSeen() {
        val puzzle = _uiState.value.puzzle ?: return
        if (_uiState.value.howToSeen) return
        _uiState.value = _uiState.value.copy(howToSeen = true)
        viewModelScope.launch { PuzzleStore.markHowToSeen(puzzle.kind) }
    }

    private fun terminal(): Boolean = _uiState.value.status.isTerminal()
    private fun PuzzleStatus.isTerminal() = this == PuzzleStatus.SOLVED || this == PuzzleStatus.FAILED
}
