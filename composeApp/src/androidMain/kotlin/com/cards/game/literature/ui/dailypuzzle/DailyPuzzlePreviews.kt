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
import com.cards.game.literature.puzzle.HalfSuitClaim
import com.cards.game.literature.puzzle.LocateCard
import com.cards.game.literature.puzzle.PuzzleKind
import com.cards.game.literature.puzzle.WastedAsk
import com.cards.game.literature.stats.PuzzleProgress
import com.cards.game.literature.stats.PuzzleStatus
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.viewmodel.DailyPuzzleUiState
import com.cards.game.literature.viewmodel.PuzzleFeedback

/**
 * Design-time harness for the Daily Puzzle screen. On a real device today's puzzle can only be
 * solved once (PuzzleStore persists the result) and only ONE kind shows per day, so to iterate on
 * the UI we drive the hoisted [DailyPuzzleScreenContent] directly with hand-built
 * [DailyPuzzleUiState] — no device, no solving, no clearing app data. Mirror of the
 * [com.cards.game.literature.ui.stats.StatsPreviews] approach.
 *
 * - Numbered static previews cover every visual state of all three kinds (light, dark, narrow).
 * - The INTERACTIVE previews are clickable mini-VMs: open them in Android Studio's *Interactive
 *   Preview* to tap through each kind's solve → result flow repeatedly.
 */

// ── Sample data (deterministic; one puzzle per kind from fixed seeds) ──────────

private val sampleClaim: DailyPuzzle =
    DailyPuzzleGenerator.generateForDay(PuzzleKind.CLAIM, 20_622L * 1_000_003L)
        ?: error("DailyPuzzlePreviews: no CLAIM puzzle for the sample seed")
private val sampleClaimAnswer = sampleClaim.answer as HalfSuitClaim
private val sampleHalfSuit: HalfSuit = sampleClaimAnswer.halfSuit
private val sampleHidden: Card = sampleClaimAnswer.hiddenCard

/** A wrong-but-tappable candidate: a target card the human does NOT hold and that isn't the hidden one. */
private val sampleWrongCard: Card =
    DeckUtils.getAllCardsForHalfSuit(sampleHalfSuit)
        .first { it != sampleHidden && it !in sampleClaim.myHand }

private val sampleLocate: DailyPuzzle =
    DailyPuzzleGenerator.generateForDay(PuzzleKind.LOCATE, 314_159L)
        ?: error("DailyPuzzlePreviews: no LOCATE puzzle for the sample seed")
private val sampleLocateAnswer = sampleLocate.answer as LocateCard

private val sampleWasted: DailyPuzzle =
    DailyPuzzleGenerator.generateForDay(PuzzleKind.WASTED_ASK, 271_828L)
        ?: error("DailyPuzzlePreviews: no WASTED_ASK puzzle for the sample seed")
private val sampleWastedAnswer = sampleWasted.answer as WastedAsk

private fun state(
    puzzle: DailyPuzzle? = sampleClaim,
    selectedHalfSuit: HalfSuit? = null,
    selectedCard: Card? = null,
    selectedSeatId: String? = null,
    status: PuzzleStatus = PuzzleStatus.NOT_STARTED,
    attemptsUsed: Int = 0,
    stars: Int = 0,
    feedback: PuzzleFeedback = PuzzleFeedback.NONE,
    revealed: Boolean = false,
    streak: Int = 7,
    loading: Boolean = false
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
    selectedSeatId = selectedSeatId,
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
            onSelectSeat = {},
            onSubmit = {},
            onHowToSeen = {}
        )
    }
}

// ── CLAIM static states ─────────────────────────────────────────────────────────

@Preview(name = "1 · Claim — pick half-suit", showBackground = true, heightDp = 900)
@Composable
private fun Step1Preview() = Render(state())

@Preview(name = "2 · Claim — clue board, no pick", showBackground = true, heightDp = 900)
@Composable
private fun Step2EmptyPreview() =
    Render(state(selectedHalfSuit = sampleHalfSuit, status = PuzzleStatus.IN_PROGRESS))

@Preview(name = "3 · Claim — card picked", showBackground = true, heightDp = 900)
@Composable
private fun Step2PickedPreview() =
    Render(state(selectedHalfSuit = sampleHalfSuit, selectedCard = sampleHidden, status = PuzzleStatus.IN_PROGRESS))

@Preview(name = "4 · Claim — wrong card", showBackground = true, heightDp = 900)
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

@Preview(name = "5 · Claim — solved, 3 stars", showBackground = true, heightDp = 900)
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

@Preview(name = "6 · Claim — solved, 1 star", showBackground = true, heightDp = 900)
@Composable
private fun Solved1Preview() =
    Render(state(status = PuzzleStatus.SOLVED, attemptsUsed = 3, stars = 1, streak = 4, revealed = true))

@Preview(name = "7 · Claim — out of tries", showBackground = true, heightDp = 900)
@Composable
private fun FailedPreview() =
    Render(state(status = PuzzleStatus.FAILED, attemptsUsed = 3, stars = 0, streak = 0, revealed = true))

@Preview(name = "8 · Loading", showBackground = true, heightDp = 400)
@Composable
private fun LoadingPreview() = Render(state(loading = true, puzzle = null, streak = 0))

@Preview(name = "9 · Claim — dark", showBackground = true, heightDp = 900)
@Composable
private fun Step2DarkPreview() =
    Render(state(selectedHalfSuit = sampleHalfSuit, selectedCard = sampleHidden, status = PuzzleStatus.IN_PROGRESS), dark = true)

@Preview(name = "10 · Claim — narrow 320dp", showBackground = true, widthDp = 320, heightDp = 900)
@Composable
private fun Step2NarrowPreview() =
    Render(state(selectedHalfSuit = sampleHalfSuit, selectedCard = sampleHidden, status = PuzzleStatus.IN_PROGRESS))

// ── LOCATE static states ────────────────────────────────────────────────────────

@Preview(name = "11 · Locate — no pick", showBackground = true, heightDp = 900)
@Composable
private fun LocateEmptyPreview() = Render(state(puzzle = sampleLocate, status = PuzzleStatus.IN_PROGRESS))

@Preview(name = "12 · Locate — seat picked", showBackground = true, heightDp = 900)
@Composable
private fun LocatePickedPreview() =
    Render(state(puzzle = sampleLocate, selectedSeatId = sampleLocateAnswer.seatId, status = PuzzleStatus.IN_PROGRESS))

@Preview(name = "13 · Locate — solved", showBackground = true, heightDp = 900)
@Composable
private fun LocateSolvedPreview() =
    Render(
        state(
            puzzle = sampleLocate,
            selectedSeatId = sampleLocateAnswer.seatId,
            status = PuzzleStatus.SOLVED,
            attemptsUsed = 1,
            stars = 3,
            streak = 9,
            revealed = true
        )
    )

@Preview(name = "14 · Locate — dark", showBackground = true, heightDp = 900)
@Composable
private fun LocateDarkPreview() =
    Render(state(puzzle = sampleLocate, status = PuzzleStatus.IN_PROGRESS), dark = true)

// ── WASTED_ASK static states ──────────────────────────────────────────────────────

@Preview(name = "15 · Wasted Ask — no pick", showBackground = true, heightDp = 900)
@Composable
private fun WastedEmptyPreview() = Render(state(puzzle = sampleWasted, status = PuzzleStatus.IN_PROGRESS))

@Preview(name = "16 · Wasted Ask — opponent picked", showBackground = true, heightDp = 900)
@Composable
private fun WastedPickedPreview() =
    Render(state(puzzle = sampleWasted, selectedSeatId = sampleWastedAnswer.seatId, status = PuzzleStatus.IN_PROGRESS))

@Preview(name = "17 · Wasted Ask — solved", showBackground = true, heightDp = 900)
@Composable
private fun WastedSolvedPreview() =
    Render(
        state(
            puzzle = sampleWasted,
            selectedSeatId = sampleWastedAnswer.seatId,
            status = PuzzleStatus.SOLVED,
            attemptsUsed = 2,
            stars = 2,
            streak = 5,
            revealed = true
        )
    )

// ── Interactive harnesses (Android Studio "Interactive Preview" / "Run preview") ──

/**
 * Self-contained, replayable mini-VM mirroring [com.cards.game.literature.viewmodel.DailyPuzzleViewModel]:
 * a correct answer solves it, and a 3rd wrong attempt ends the day, exactly like the real app.
 */
@Preview(name = "0 · INTERACTIVE — Claim solve flow", showBackground = true, heightDp = 950)
@Composable
private fun ClaimInteractivePreview() {
    val puzzle = sampleClaim
    val answer = sampleClaimAnswer
    var halfSuit by remember { mutableStateOf<HalfSuit?>(null) }
    var card by remember { mutableStateOf<Card?>(null) }
    var attempts by remember { mutableStateOf(0) }
    var stars by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(PuzzleStatus.NOT_STARTED) }
    var feedback by remember { mutableStateOf(PuzzleFeedback.NONE) }

    LiteratureTheme {
        DailyPuzzleScreenContent(
            uiState = state(
                puzzle = puzzle,
                selectedHalfSuit = halfSuit,
                selectedCard = card,
                status = status,
                attemptsUsed = attempts,
                stars = stars,
                feedback = feedback,
                revealed = status == PuzzleStatus.SOLVED || status == PuzzleStatus.FAILED
            ),
            onBack = {},
            onSelectHalfSuit = { halfSuit = it; card = null; feedback = PuzzleFeedback.NONE },
            onChangeHalfSuit = { halfSuit = null; card = null; feedback = PuzzleFeedback.NONE },
            onSelectCard = { card = it; feedback = PuzzleFeedback.NONE },
            onSelectSeat = {},
            onSubmit = {
                val picked = card
                if (status != PuzzleStatus.SOLVED && status != PuzzleStatus.FAILED && picked != null) {
                    val rightSuit = halfSuit == answer.halfSuit
                    val correct = rightSuit && picked == answer.hiddenCard
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

/** Shared one-tap interactive flow for LOCATE / WASTED_ASK. */
@Composable
private fun SeatKindInteractive(puzzle: DailyPuzzle, answerSeatId: String) {
    var seat by remember { mutableStateOf<String?>(null) }
    var attempts by remember { mutableStateOf(0) }
    var stars by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf(PuzzleStatus.NOT_STARTED) }
    var feedback by remember { mutableStateOf(PuzzleFeedback.NONE) }

    LiteratureTheme {
        DailyPuzzleScreenContent(
            uiState = state(
                puzzle = puzzle,
                selectedSeatId = seat,
                status = status,
                attemptsUsed = attempts,
                stars = stars,
                feedback = feedback,
                revealed = status == PuzzleStatus.SOLVED || status == PuzzleStatus.FAILED
            ),
            onBack = {},
            onSelectHalfSuit = {},
            onChangeHalfSuit = {},
            onSelectCard = {},
            onSelectSeat = { seat = it; feedback = PuzzleFeedback.NONE },
            onSubmit = {
                val picked = seat
                if (status != PuzzleStatus.SOLVED && status != PuzzleStatus.FAILED && picked != null) {
                    attempts += 1
                    when {
                        picked == answerSeatId -> { status = PuzzleStatus.SOLVED; stars = PuzzleProgress.starsFor(attempts) }
                        attempts >= PuzzleProgress.MAX_ATTEMPTS -> status = PuzzleStatus.FAILED
                        else -> feedback = PuzzleFeedback.WRONG_SEAT
                    }
                }
            },
            onHowToSeen = {}
        )
    }
}

@Preview(name = "0 · INTERACTIVE — Locate solve flow", showBackground = true, heightDp = 950)
@Composable
private fun LocateInteractivePreview() = SeatKindInteractive(sampleLocate, sampleLocateAnswer.seatId)

@Preview(name = "0 · INTERACTIVE — Wasted Ask solve flow", showBackground = true, heightDp = 950)
@Composable
private fun WastedInteractivePreview() = SeatKindInteractive(sampleWasted, sampleWastedAnswer.seatId)
