package com.cards.game.literature.puzzle

import kotlinx.serialization.Serializable

/**
 * The kind of daily puzzle. Each kind has its own generator recipe (see
 * [DailyPuzzleGenerator]), solve interaction, and how-to. The day's kind is chosen
 * by [PuzzleSchedule] and stays fixed while the seed forward-searches for solvability.
 *
 * Only kinds whose answer is a single *provably-forced* function of the deductive
 * [com.cards.game.literature.logic.CardTracker] output are fair — the engine has no
 * probability/EV, so "best/optimal ask" style questions are deliberately absent.
 */
@Serializable
enum class PuzzleKind {
    /** Two taps: which half-suit your team can claim, then the card the teammate hides. */
    CLAIM,

    /** One tap: which seat must be holding a named card (forced by the 5-of-6 deduction). */
    LOCATE,

    /** One tap: which opponent is provably unable to hold a card you'd ask for (a wasted ask). */
    WASTED_ASK
}
