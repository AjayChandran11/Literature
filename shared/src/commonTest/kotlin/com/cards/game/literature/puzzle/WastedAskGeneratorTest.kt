package com.cards.game.literature.puzzle

import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.CardTrackerState
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.Player
import com.cards.game.literature.model.Suit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WastedAskGeneratorTest {

    private val opponents = listOf("player_1", "player_3")

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
    fun exactlyOneOpponentIsProvablyRuledOut() {
        var produced = 0
        for (seed in 0L until 200L) {
            val p = DailyPuzzleGenerator.generate(PuzzleKind.WASTED_ASK, seed) ?: continue
            produced++
            assertEquals(PuzzleKind.WASTED_ASK, p.kind, "seed=$seed kind")
            val ans = p.answer as WastedAsk
            val state = publicState(p)

            // The card's location must stay uncertain — otherwise the question degenerates.
            assertNull(state.knownLocations[ans.card], "seed=$seed card location is known")
            // EXACTLY the answer opponent is provably ruled out; the other is still possible.
            val ruled = opponents.filter { it in (state.impossibleLocations[ans.card] ?: emptySet()) }
            assertEquals(listOf(ans.seatId), ruled, "seed=$seed ruled-out opponents")
            assertTrue(ans.seatId in opponents, "seed=$seed answer is not an opponent")
            // A legal ask to pose: the human holds a suit-mate but not the card itself.
            val hs = DeckUtils.getAllCardsForHalfSuit(DeckUtils.getHalfSuit(ans.card)).toSet()
            assertTrue(p.myHand.any { it in hs }, "seed=$seed human can't legally ask for this suit")
            assertTrue(ans.card !in p.myHand, "seed=$seed human already holds the card")
        }
        assertTrue(produced >= 10, "expected several WASTED_ASK puzzles from 200 seeds, got $produced")
    }

    @Test
    fun logShowsCardsChangingHands() {
        // The liven step adds legal successful transfers in OTHER suits so the log isn't all misses.
        // Not every seed can host one, but a healthy share should — and the deduction must survive.
        var produced = 0
        var withTransfer = 0
        for (seed in 0L until 200L) {
            val p = DailyPuzzleGenerator.generate(PuzzleKind.WASTED_ASK, seed) ?: continue
            produced++
            val ans = p.answer as WastedAsk
            val answerSuit = DeckUtils.getHalfSuit(ans.card)
            val transfers = p.events.filterIsInstance<GameEvent.CardAsked>().filter { it.success }
            if (transfers.isNotEmpty()) withTransfer++
            // Every visible transfer stays out of the answer's suit, so it can't move the asked card.
            transfers.forEach {
                assertTrue(
                    DeckUtils.getHalfSuit(it.card) != answerSuit,
                    "seed=$seed a transfer touched the answer's suit"
                )
            }
        }
        assertTrue(withTransfer >= produced / 2, "expected most logs to show a transfer; $withTransfer/$produced")
    }

    @Test
    fun generationIsDeterministic() {
        for (seed in 100L until 140L) {
            assertEquals(
                DailyPuzzleGenerator.generate(PuzzleKind.WASTED_ASK, seed),
                DailyPuzzleGenerator.generate(PuzzleKind.WASTED_ASK, seed),
                "seed=$seed must be reproducible"
            )
        }
    }
}
