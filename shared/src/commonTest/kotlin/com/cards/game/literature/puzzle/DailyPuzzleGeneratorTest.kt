package com.cards.game.literature.puzzle

import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.Player
import com.cards.game.literature.model.Suit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DailyPuzzleGeneratorTest {

    private val team1 = setOf("player_0", "player_2")

    /**
     * Re-derive what the solver can know from ONLY the puzzle's public view: the
     * human's hand + the public log + each seat's card count (→ isActive). Other
     * seats get filler hands of the right size — CardTracker never reads their
     * contents, only their size — so this faithfully mirrors the solver.
     */
    private fun publicKnown(p: DailyPuzzle): Map<Card, String> {
        val filler = Card(Suit.SPADES, CardValue.TWO)
        val players = p.players.map { pp ->
            Player(
                id = pp.id, name = pp.name, teamId = pp.teamId,
                hand = if (pp.id == p.humanSeatId) p.myHand else List(pp.cardCount) { filler }
            )
        }
        return CardTracker().buildState(p.events, players, p.humanSeatId).knownLocations
    }

    @Test
    fun generatedPuzzleIsSolvableFromPublicView() {
        var produced = 0
        for (seed in 0L until 80L) {
            val p = DailyPuzzleGenerator.generate(PuzzleKind.CLAIM, seed) ?: continue
            produced++
            assertEquals(PuzzleKind.CLAIM, p.kind, "seed=$seed kind")
            val ans = p.answer as HalfSuitClaim
            assertEquals(6, ans.holders.size, "seed=$seed holders")
            val hsCards = DeckUtils.getAllCardsForHalfSuit(ans.halfSuit).toSet()
            assertEquals(hsCards, ans.holders.map { it.card }.toSet(), "seed=$seed half-suit cards")
            assertTrue(ans.holders.all { it.playerId in team1 }, "seed=$seed all on team_1")

            val known = publicKnown(p)
            for (h in ans.holders) {
                assertEquals(h.playerId, known[h.card], "seed=$seed deduce ${h.card}")
            }
            assertTrue(ans.holders.any { it.card !in p.myHand }, "seed=$seed needs deduction")

            // The single deduced card (step 2): teammate's, not in your hand, in the half-suit,
            // with >=2 non-hand cards so the pick is a real choice (>=1 distractor + the answer).
            val hidden = ans.hiddenCard
            assertTrue(hidden in hsCards, "seed=$seed hidden in half-suit")
            assertTrue(hidden !in p.myHand, "seed=$seed hidden not in hand")
            assertEquals("player_2", ans.holderOf(hidden), "seed=$seed hidden is teammate's")
            assertTrue(hsCards.count { it !in p.myHand } >= 2, "seed=$seed step-2 real choice")
        }
        assertTrue(produced >= 10, "expected several puzzles from 80 seeds, got $produced")
    }

    @Test
    fun generationIsDeterministic() {
        for (seed in 100L until 140L) {
            assertEquals(
                DailyPuzzleGenerator.generate(PuzzleKind.CLAIM, seed),
                DailyPuzzleGenerator.generate(PuzzleKind.CLAIM, seed),
                "seed=$seed must be reproducible"
            )
        }
    }

    @Test
    fun forwardSearchAlwaysFindsAPuzzle() {
        for (day in 0L until 50L) {
            assertNotNull(DailyPuzzleGenerator.generateForDay(PuzzleKind.CLAIM, day * 1000L), "day=$day")
        }
    }

    @Test
    fun logIsNotASingleSuitGiveaway() {
        var checked = 0
        for (seed in 0L until 200L) {
            val p = DailyPuzzleGenerator.generate(PuzzleKind.CLAIM, seed) ?: continue
            checked++
            val ans = p.answer as HalfSuitClaim
            val suitsInLog = p.events
                .filterIsInstance<GameEvent.CardAsked>()
                .map { DeckUtils.getHalfSuit(it.card) }
                .toSet()
            assertTrue(ans.halfSuit in suitsInLog, "seed=$seed answer suit must appear in the log")
            assertTrue(
                suitsInLog.size >= 2,
                "seed=$seed log should span >=2 half-suits (decoys), not just the answer; got $suitsInLog"
            )
        }
        assertTrue(checked >= 10, "expected several puzzles from 200 seeds, got $checked")
    }

    @Test
    fun decoysCanBeDisabledForEasierTiers() {
        // difficulty = 0 reproduces the old single-suit log — the easy end of the future ramp.
        var checked = 0
        for (seed in 0L until 200L) {
            val p = DailyPuzzleGenerator.generate(PuzzleKind.CLAIM, seed, difficulty = 0) ?: continue
            checked++
            val ans = p.answer as HalfSuitClaim
            val suits = p.events.filterIsInstance<GameEvent.CardAsked>()
                .map { DeckUtils.getHalfSuit(it.card) }.toSet()
            assertEquals(setOf(ans.halfSuit), suits, "seed=$seed decoys off → single suit")
        }
        assertTrue(checked >= 10, "expected several puzzles, got $checked")
    }
}
