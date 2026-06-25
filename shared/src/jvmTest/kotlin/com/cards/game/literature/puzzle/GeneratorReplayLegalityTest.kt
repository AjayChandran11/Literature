package com.cards.game.literature.puzzle

import com.cards.game.literature.logic.CardDealer
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.GameEvent
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Adversarial sweep (JVM only, not shipped): for many seeds of every [PuzzleKind], reconstruct the
 * REAL initial deal — `CardDealer.dealCards(4, seed)`, exactly what the generator starts from — then
 * replay the public ask log and assert at EVERY step that the move was legal and consistent:
 *  - the ask is cross-team (in Literature you may only ask an opponent),
 *  - the asker holds a suit-mate of the asked card and does NOT already hold it,
 *  - a success happens only when the target actually holds the card; a miss only when it doesn't,
 * and finally that the recorded answer matches GROUND TRUTH (the true final holder), not merely the
 * public deduction. This is stronger than the public-view tests: it also checks the hidden hands.
 */
class GeneratorReplayLegalityTest {

    private val team1 = setOf("player_0", "player_2")
    private fun teamOf(id: String) = if (id in team1) "team_1" else "team_2"

    @Test
    fun everyAskIsLegalAndEveryAnswerMatchesGroundTruth() {
        val ids = listOf("player_0", "player_1", "player_2", "player_3")
        val problems = mutableListOf<String>()
        var checked = 0

        for (kind in PuzzleKind.entries) {
            for (seed in 0L until 2000L) {
                val p = DailyPuzzleGenerator.generate(kind, seed) ?: continue
                checked++
                // The generator deals from CardDealer.dealCards(4, seed); start from the same hands.
                val dealt = CardDealer.dealCards(4, seed)
                val hand = ids.mapIndexed { i, id -> id to dealt[i].toMutableSet() }.toMap()

                for (e in p.events) {
                    if (e !is GameEvent.CardAsked) continue
                    val hs = DeckUtils.getAllCardsForHalfSuit(DeckUtils.getHalfSuit(e.card)).toSet()
                    if (teamOf(e.askerId) == teamOf(e.targetId))
                        problems += "$kind/$seed same-team ask ${e.askerId}->${e.targetId}"
                    if (hand.getValue(e.askerId).none { it in hs })
                        problems += "$kind/$seed asker ${e.askerId} holds no suit-mate of ${e.card}"
                    if (e.card in hand.getValue(e.askerId))
                        problems += "$kind/$seed asker ${e.askerId} already holds ${e.card}"
                    if (e.success) {
                        if (e.card !in hand.getValue(e.targetId))
                            problems += "$kind/$seed success but ${e.targetId} lacks ${e.card}"
                        hand.getValue(e.targetId).remove(e.card)
                        hand.getValue(e.askerId).add(e.card)
                    } else if (e.card in hand.getValue(e.targetId)) {
                        problems += "$kind/$seed miss but ${e.targetId} holds ${e.card}"
                    }
                }

                when (val a = p.answer) {
                    is HalfSuitClaim -> {
                        if (a.hiddenCard !in hand.getValue("player_2"))
                            problems += "CLAIM/$seed hidden card not at player_2"
                        a.holders.forEach { h ->
                            if (h.card !in hand.getValue(h.playerId)) problems += "CLAIM/$seed holder mismatch ${h.card}"
                        }
                    }
                    is LocateCard -> if (a.card !in hand.getValue(a.seatId))
                        problems += "LOCATE/$seed ${a.card} not actually at ${a.seatId}"
                    is WastedAsk -> if (a.card in hand.getValue(a.seatId))
                        problems += "WASTED/$seed ruled-out seat ${a.seatId} actually holds ${a.card}"
                }
            }
        }
        assertTrue(checked >= 100, "expected many puzzles across kinds, got $checked")
        assertTrue(
            problems.isEmpty(),
            "replay found ${problems.size} legality/ground-truth problem(s):\n" + problems.take(20).joinToString("\n")
        )
    }
}
