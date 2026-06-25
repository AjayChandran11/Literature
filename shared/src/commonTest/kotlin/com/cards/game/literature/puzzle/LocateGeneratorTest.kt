package com.cards.game.literature.puzzle

import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.CardTrackerState
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.Player
import com.cards.game.literature.model.Suit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LocateGeneratorTest {

    /** The solver's deduction oracle from ONLY the public view (own hand + log + seat counts). */
    private fun publicState(p: DailyPuzzle): CardTrackerState {
        val filler = Card(Suit.SPADES, CardValue.TWO)
        val players = p.players.map { pp ->
            Player(
                id = pp.id, name = pp.name, teamId = pp.teamId,
                hand = if (pp.id == p.humanSeatId) p.myHand else List(pp.cardCount) { filler }
            )
        }
        return CardTracker().buildState(p.events, players, p.humanSeatId)
    }

    @Test
    fun locatedSeatIsForcedFromPublicView() {
        // Holds for BOTH flavours: whether the card is deduced (never moves) or tracked (moves via
        // successful asks), the answer is exactly the seat CardTracker forces it to.
        var produced = 0
        for (seed in 0L until 200L) {
            val p = DailyPuzzleGenerator.generate(PuzzleKind.LOCATE, seed) ?: continue
            produced++
            assertEquals(PuzzleKind.LOCATE, p.kind, "seed=$seed kind")
            val ans = p.answer as LocateCard

            // Not in the human's own hand (otherwise the answer is just "me").
            assertTrue(ans.card !in p.myHand, "seed=$seed card is in the human's hand")
            // The answer is some other seat — never the human.
            assertNotEquals(p.humanSeatId, ans.seatId, "seed=$seed answer seat is the human")
            // Forced from the public view: the deduction oracle pins it to exactly that seat.
            val state = publicState(p)
            assertEquals(ans.seatId, state.knownLocations[ans.card], "seed=$seed not forced to its seat")
        }
        assertTrue(produced >= 10, "expected several LOCATE puzzles from 200 seeds, got $produced")
    }

    @Test
    fun mixesEliminationAndTransferFlavours() {
        // Some days the answer card never changes hands (deduce the unseen 6th); some days it
        // visibly moves through the log (track it). Both flavours must appear across the seed space.
        var withTransfer = 0
        var withoutTransfer = 0
        for (seed in 0L until 400L) {
            val p = DailyPuzzleGenerator.generate(PuzzleKind.LOCATE, seed) ?: continue
            val ans = p.answer as LocateCard
            val seenMoving = p.events.filterIsInstance<GameEvent.CardAsked>().any { it.success && it.card == ans.card }
            if (seenMoving) withTransfer++ else withoutTransfer++
        }
        assertTrue(withTransfer >= 5, "expected some tracked-transfer locates, got $withTransfer")
        assertTrue(withoutTransfer >= 5, "expected some elimination locates, got $withoutTransfer")
    }

    @Test
    fun generationIsDeterministic() {
        for (seed in 100L until 140L) {
            assertEquals(
                DailyPuzzleGenerator.generate(PuzzleKind.LOCATE, seed),
                DailyPuzzleGenerator.generate(PuzzleKind.LOCATE, seed),
                "seed=$seed must be reproducible"
            )
        }
    }
}
