package com.cards.game.literature.ui.dailypuzzle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.puzzle.DailyPuzzle
import com.cards.game.literature.puzzle.DailyPuzzleGenerator
import com.cards.game.literature.stats.PuzzleProgress
import com.cards.game.literature.stats.PuzzleStatus
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.viewmodel.DailyPuzzleUiState
import com.cards.game.literature.viewmodel.PuzzleFeedback

/**
 * Design-time harness for the Daily Puzzle screen. On a real device today's puzzle can only be
 * solved once (PuzzleStore persists the result), so to iterate on the UI we drive the hoisted
 * [DailyPuzzleScreenContent] directly with hand-built [DailyPuzzleUiState] — no device, no solving,
 * no clearing app data. Mirror of the [com.cards.game.literature.ui.stats.StatsPreviews] approach.
 *
 * - Numbered static previews below cover every visual state (light, dark, narrow).
 * - [DailyPuzzleInteractivePreview] is a clickable mini-VM: open it in Android Studio's
 *   *Interactive Preview* to tap through pick-half-suit → clue board → claim → result repeatedly.
 */

// ── Sample data (deterministic; generated from a fixed seed) ──────────────────

private val samplePuzzle: DailyPuzzle =
    DailyPuzzleGenerator.generateForDay(20_622L * 1_000_003L)
        ?: error("DailyPuzzlePreviews: generator produced no puzzle for the sample seed")

private val sampleHalfSuit: HalfSuit = samplePuzzle.answer.halfSuit
private val sampleHidden: Card = samplePuzzle.answer.hiddenCard

/** A wrong-but-tappable candidate: a target card the human does NOT hold and that isn't the hidden one. */
private val sampleWrongCard: Card =
    DeckUtils.getAllCardsForHalfSuit(sampleHalfSuit)
        .first { it != sampleHidden && it !in samplePuzzle.myHand }

private fun state(
    selectedHalfSuit: HalfSuit? = null,
    selectedCard: Card? = null,
    status: PuzzleStatus = PuzzleStatus.NOT_STARTED,
    attemptsUsed: Int = 0,
    stars: Int = 0,
    feedback: PuzzleFeedback = PuzzleFeedback.NONE,
    revealed: Boolean = false,
    streak: Int = 7,
    loading: Boolean = false,
    puzzle: DailyPuzzle? = samplePuzzle
) = DailyPuzzleUiState(
    loading = loading,
    puzzle = puzzle,
    puzzleNumber = 128,
    streak = streak,
    status = status,
    attemptsUsed = attemptsUsed,
    stars = stars,
    selectedHalfSuit = selectedHalfSuit,
    selectedCard = selectedCard,
    feedback = feedback,
    revealed = revealed,
    howToSeen = true // keep the auto how-to dialog out of previews
)

@Composable
private fun Render(ui: DailyPuzzleUiState, dark: Boolean = false) {
    LiteratureTheme(darkTheme = dark) {
        DailyPuzzleScreenContent(
            uiState = ui,
            onBack = {},
            onSelectHalfSuit = {},
            onChangeHalfSuit = {},
            onSelectCard = {},
            onSubmit = {},
            onHowToSeen = {}
        )
    }
}

// ── Static states ─────────────────────────────────────────────────────────────

@Preview(name = "1 · Step 1 — pick half-suit", showBackground = true, heightDp = 900)
@Composable
private fun Step1Preview() = Render(state())

@Preview(name = "2 · Step 2 — clue board, no pick", showBackground = true, heightDp = 900)
@Composable
private fun Step2EmptyPreview() =
    Render(state(selectedHalfSuit = sampleHalfSuit, status = PuzzleStatus.IN_PROGRESS))

@Preview(name = "3 · Step 2 — card picked", showBackground = true, heightDp = 900)
@Composable
private fun Step2PickedPreview() =
    Render(state(selectedHalfSuit = sampleHalfSuit, selectedCard = sampleHidden, status = PuzzleStatus.IN_PROGRESS))

@Preview(name = "4 · Step 2 — wrong card", showBackground = true, heightDp = 900)
@Composable
private fun Step2WrongPreview() =
    Render(
        state(
            selectedHalfSuit = sampleHalfSuit,
            selectedCard = sampleWrongCard,
            feedback = PuzzleFeedback.WRONG_CARD,
            attemptsUsed = 1,
            status = PuzzleStatus.IN_PROGRESS
        )
    )

@Preview(name = "5 · Solved — 3 stars", showBackground = true, heightDp = 900)
@Composable
private fun Solved3Preview() =
    Render(
        state(
            selectedHalfSuit = sampleHalfSuit,
            selectedCard = sampleHidden,
            status = PuzzleStatus.SOLVED,
            attemptsUsed = 1,
            stars = 3,
            streak = 8,
            revealed = true
        )
    )

@Preview(name = "6 · Solved — 1 star", showBackground = true, heightDp = 900)
@Composable
private fun Solved1Preview() =
    Render(state(status = PuzzleStatus.SOLVED, attemptsUsed = 3, stars = 1, streak = 4, revealed = true))

@Preview(name = "7 · Out of tries", showBackground = true, heightDp = 900)
@Composable
private fun FailedPreview() =
    Render(state(status = PuzzleStatus.FAILED, attemptsUsed = 3, stars = 0, streak = 0, revealed = true))

@Preview(name = "8 · Loading", showBackground = true, heightDp = 400)
@Composable
private fun LoadingPreview() = Render(state(loading = true, puzzle = null, streak = 0))

@Preview(name = "9 · Step 2 — dark", showBackground = true, heightDp = 900)
@Composable
private fun Step2DarkPreview() =
    Render(state(selectedHalfSuit = sampleHalfSuit, selectedCard = sampleHidden, status = PuzzleStatus.IN_PROGRESS), dark = true)

@Preview(name = "10 · Step 2 — narrow 320dp", showBackground = true, widthDp = 320, heightDp = 900)
@Composable
private fun Step2NarrowPreview() =
    Render(state(selectedHalfSuit = sampleHalfSuit, selectedCard = sampleHidden, status = PuzzleStatus.IN_PROGRESS))

// ── Interactive harness (use Android Studio "Interactive Preview" / "Run preview") ──

/**
 * A self-contained, replayable mini-VM that faithfully mirrors [com.cards.game.literature.viewmodel.DailyPuzzleViewModel]:
 * a correct claim solves it, and a 3rd wrong attempt ends the day ("Out of tries"), exactly like
 * the real app. To run the flow again, re-launch the preview.
 */
@Preview(name = "0 · INTERACTIVE — full solve flow", showBackground = true, heightDp = 950)
@Composable
private fun DailyPuzzleInteractivePreview() {
    val puzzle = samplePuzzle
    var halfSuit by remember { mutableStateOf<HalfSuit?>(null) }
    var card by remember { mutableStateOf<Card?>(null) }
    var attempts by remember { mutableStateOf(0) }
    var stars by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(PuzzleStatus.NOT_STARTED) }
    var feedback by remember { mutableStateOf(PuzzleFeedback.NONE) }

    LiteratureTheme {
        DailyPuzzleScreenContent(
            uiState = DailyPuzzleUiState(
                loading = false,
                puzzle = puzzle,
                puzzleNumber = 128,
                streak = 7,
                status = status,
                attemptsUsed = attempts,
                stars = stars,
                selectedHalfSuit = halfSuit,
                selectedCard = card,
                feedback = feedback,
                revealed = status == PuzzleStatus.SOLVED || status == PuzzleStatus.FAILED,
                howToSeen = true
            ),
            onBack = {},
            onSelectHalfSuit = { halfSuit = it; card = null; feedback = PuzzleFeedback.NONE },
            onChangeHalfSuit = { halfSuit = null; card = null; feedback = PuzzleFeedback.NONE },
            onSelectCard = { card = it; feedback = PuzzleFeedback.NONE },
            onSubmit = {
                val picked = card
                if (status != PuzzleStatus.SOLVED && status != PuzzleStatus.FAILED && picked != null) {
                    val rightSuit = halfSuit == puzzle.answer.halfSuit
                    val correct = rightSuit && picked == puzzle.answer.hiddenCard
                    attempts += 1
                    when {
                        correct -> { status = PuzzleStatus.SOLVED; stars = PuzzleProgress.starsFor(attempts) }
                        attempts >= PuzzleProgress.MAX_ATTEMPTS -> status = PuzzleStatus.FAILED
                        !rightSuit -> feedback = PuzzleFeedback.WRONG_HALF_SUIT
                        else -> feedback = PuzzleFeedback.WRONG_CARD
                    }
                }
            },
            onHowToSeen = {}
        )
    }
}
