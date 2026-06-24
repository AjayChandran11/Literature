package com.cards.game.literature.puzzle

import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.HalfSuitStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single solver-facing seat in a puzzle snapshot. The solver only ever sees
 * card *counts* for other seats (their hands stay hidden) — exactly what a real
 * player sees at the table. The human seat's own hand lives in [DailyPuzzle.myHand].
 */
@Serializable
data class PuzzlePlayer(
    val id: String,
    val name: String,
    val teamId: String,
    val cardCount: Int
)

/** One card and the seat that actually holds it (the solution key). */
@Serializable
data class CardHolder(val card: Card, val playerId: String)

/**
 * The correct solution for a puzzle. One subtype per [PuzzleKind]; the solve screen and
 * the [com.cards.game.literature.viewmodel.DailyPuzzleViewModel] branch on the concrete
 * type. Sealed (closed polymorphism) so kotlinx-serialization needs no module registration.
 */
@Serializable
sealed interface PuzzleAnswer

/**
 * [PuzzleKind.CLAIM]: the half-suit now fully claimable by the human's team, who holds each
 * of its six cards, and the single card the solver must *deduce* — held by the teammate and
 * never shown being acquired in the log (the others changed hands on-screen). Step 2 asks for it.
 */
@Serializable
@SerialName("claim")
data class HalfSuitClaim(
    val halfSuit: HalfSuit,
    val holders: List<CardHolder>,
    val hiddenCard: Card
) : PuzzleAnswer {
    /** Holder seat id for a given card, or null if the card isn't part of this half-suit. */
    fun holderOf(card: Card): String? = holders.firstOrNull { it.card == card }?.playerId
}

/**
 * [PuzzleKind.LOCATE]: a named [card] whose holder ([seatId]) is forced by the public log
 * (the 5-of-6 deduction). The solver taps the seat that must hold it.
 */
@Serializable
@SerialName("locate")
data class LocateCard(
    val card: Card,
    val seatId: String
) : PuzzleAnswer

/**
 * [PuzzleKind.WASTED_ASK]: a [card] the human could legally ask for, and the one opponent
 * ([seatId]) provably unable to hold it — so asking them would be wasted. The solver taps
 * that opponent.
 */
@Serializable
@SerialName("wasted")
data class WastedAsk(
    val card: Card,
    val seatId: String
) : PuzzleAnswer

/**
 * A self-contained daily deduction puzzle, produced deterministically from [seed]
 * by [DailyPuzzleGenerator]. Everything the solve screen needs is captured here:
 * the human's hand, the public ask/claim log, per-seat card counts, the already
 * claimed half-suits, and the answer key for local validation.
 *
 * The position is always solvable purely from [myHand] + [events] via CardTracker —
 * that invariant is what the generator guarantees before emitting a puzzle.
 */
@Serializable
data class DailyPuzzle(
    val seed: Long,
    val kind: PuzzleKind,
    val playerCount: Int,
    val players: List<PuzzlePlayer>,
    val humanSeatId: String,
    val humanTeamId: String,
    val myHand: List<Card>,
    val halfSuitStatuses: List<HalfSuitStatus>,
    val events: List<GameEvent>,
    val answer: PuzzleAnswer
)
