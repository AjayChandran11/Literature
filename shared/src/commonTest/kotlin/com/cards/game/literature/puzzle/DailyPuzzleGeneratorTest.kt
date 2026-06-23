package com.cards.game.literature.puzzle

import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
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
            val p = DailyPuzzleGenerator.generate(seed) ?: continue
            produced++
            assertEquals(6, p.answer.holders.size, "seed=$seed holders")
            val hsCards = DeckUtils.getAllCardsForHalfSuit(p.answer.halfSuit).toSet()
            assertEquals(hsCards, p.answer.holders.map { it.card }.toSet(), "seed=$seed half-suit cards")
            assertTrue(p.answer.holders.all { it.playerId in team1 }, "seed=$seed all on team_1")

            val known = publicKnown(p)
            for (h in p.answer.holders) {
                assertEquals(h.playerId, known[h.card], "seed=$seed deduce ${h.card}")
            }
            assertTrue(p.answer.holders.any { it.card !in p.myHand }, "seed=$seed needs deduction")

            // The single deduced card (step 2): teammate's, not in your hand, in the half-suit,
            // with >=2 non-hand cards so the pick is a real choice (>=1 distractor + the answer).
            val hidden = p.answer.hiddenCard
            assertTrue(hidden in hsCards, "seed=$seed hidden in half-suit")
            assertTrue(hidden !in p.myHand, "seed=$seed hidden not in hand")
            assertEquals("player_2", p.answer.holderOf(hidden), "seed=$seed hidden is teammate's")
            assertTrue(hsCards.count { it !in p.myHand } >= 2, "seed=$seed step-2 real choice")
        }
        assertTrue(produced >= 10, "expected several puzzles from 80 seeds, got $produced")
    }

    @Test
    fun generationIsDeterministic() {
        for (seed in 100L until 140L) {
            assertEquals(
                DailyPuzzleGenerator.generate(seed),
                DailyPuzzleGenerator.generate(seed),
                "seed=$seed must be reproducible"
            )
        }
    }

    @Test
    fun forwardSearchAlwaysFindsAPuzzle() {
        for (day in 0L until 50L) {
            assertNotNull(DailyPuzzleGenerator.generateForDay(day * 1000L), "day=$day")
        }
    }
}
