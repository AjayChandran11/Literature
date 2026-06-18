package com.cards.game.literature.puzzle

import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.HalfSuitStatus
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
 * The correct solution: which half-suit is now fully claimable by the human's
 * team, and who holds each of its six cards.
 */
@Serializable
data class PuzzleAnswer(
    val halfSuit: HalfSuit,
    val holders: List<CardHolder>
) {
    /** Holder seat id for a given card, or null if the card isn't part of this half-suit. */
    fun holderOf(card: Card): String? = holders.firstOrNull { it.card == card }?.playerId
}

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
    val playerCount: Int,
    val players: List<PuzzlePlayer>,
    val humanSeatId: String,
    val humanTeamId: String,
    val myHand: List<Card>,
    val halfSuitStatuses: List<HalfSuitStatus>,
    val events: List<GameEvent>,
    val answer: PuzzleAnswer
)
