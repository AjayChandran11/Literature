package com.cards.game.literature.puzzle

import com.cards.game.literature.logic.CardDealer
import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.model.Player
import kotlin.random.Random

/**
 * Produces a [DailyPuzzle] deterministically from a seed. The output depends ONLY
 * on the seed (the deal is seeded via [CardDealer]; every choice draws from a seeded
 * [Random]) — so the same calendar day yields the same puzzle on every device with
 * no bundled data and no server. Frozen post-launch behind [PUZZLE_SEASON].
 *
 * Construction (4 players, human = player_0, team_1 = {player_0, player_2}):
 *  1. Pick a target half-suit where the teammate (player_2) holds exactly ONE card —
 *     that single card becomes the one the solver must *deduce*. The human holds >=1
 *     (so they can legally ask) and team_2 holds >=1 (so there's something to pull).
 *  2. Pull phase: team_1 grabs every target card team_2 holds via successful asks
 *     (each logged, so those locations are directly known).
 *  3. Probe phase: the human makes failed asks for the teammate's hidden card against
 *     both opponents — ruling out the human (asker) and both opponents, leaving the
 *     teammate as the only candidate.
 *  4. Validate with [CardTracker] from the human's POV: all six target cards must be
 *     known, each to its true holder, and every holder on team_1 (a winning claim).
 *     [CardTracker] only reads the human's hand + the public log + seat card-counts,
 *     so this guarantees the puzzle is solvable from exactly what the solver sees.
 */
object DailyPuzzleGenerator {

    /** Bump deliberately if generator heuristics change (keeps "same for everyone" within a version). */
    const val PUZZLE_SEASON = 1

    private const val PLAYER_COUNT = 4
    private const val HUMAN = "player_0"
    private const val HUMAN_TEAM = "team_1"
    private val SEAT_IDS = listOf("player_0", "player_1", "player_2", "player_3")
    private val SEAT_NAMES = mapOf(
        "player_0" to "You", "player_1" to "Ravi", "player_2" to "Tara", "player_3" to "Meera"
    )
    private val TEAM1 = listOf("player_0", "player_2")
    private val TEAM2 = listOf("player_1", "player_3")
    private fun teamOf(id: String) = if (id in TEAM1) "team_1" else "team_2"

    /** First solvable puzzle at or after [seed] — the on-device forward-search. */
    fun generateForDay(seed: Long, maxAttempts: Int = 200): DailyPuzzle? {
        for (i in 0 until maxAttempts) generate(seed + i)?.let { return it }
        return null
    }

    /** One deterministic attempt for [seed]; null if this seed yields no good puzzle. */
    fun generate(seed: Long): DailyPuzzle? {
        val rng = Random(seed xor (PUZZLE_SEASON.toLong() * -0x61c8864680b583ebL))
        val dealt = CardDealer.dealCards(PLAYER_COUNT, seed)
        val hands: Map<String, MutableList<Card>> =
            SEAT_IDS.mapIndexed { i, id -> id to dealt[i].toMutableList() }.toMap()

        // Target half-suit: teammate holds exactly 1 (the single deduced card),
        // human holds >=1 (legal asker + own known cards), team_2 holds >=1 (a pull).
        val candidates = HalfSuit.entries.filter { hs ->
            val cards = DeckUtils.getAllCardsForHalfSuit(hs).toSet()
            val p2 = hands.getValue("player_2").count { it in cards }
            val p0 = hands.getValue("player_0").count { it in cards }
            val t2 = TEAM2.sumOf { o -> hands.getValue(o).count { it in cards } }
            p2 == 1 && p0 >= 1 && t2 >= 1
        }
        if (candidates.isEmpty()) return null
        val target = candidates[rng.nextInt(candidates.size)]
        val targetCards = DeckUtils.getAllCardsForHalfSuit(target).toSet()
        val hiddenCard = hands.getValue("player_2").first { it in targetCards }

        val events = mutableListOf<GameEvent>()
        var ts = 0L // deterministic timestamps (no wall clock)

        // ── Pull phase ──────────────────────────────────────────────────────
        var guard = 0
        while (true) {
            val loot = TEAM2.flatMap { o -> hands.getValue(o).filter { it in targetCards }.map { o to it } }
            if (loot.isEmpty()) break
            if (guard++ > 12) return null
            val (owner, card) = loot[rng.nextInt(loot.size)]
            // The teammate (player_2) does the pulling, so their grabbed cards are visible
            // in the log; that leaves exactly one unseen card — the hidden one — for step 2.
            // player_2 is always a legal asker here: holds >=1 target card and never the loot.
            val asker = "player_2"
            hands.getValue(owner).remove(card)
            hands.getValue(asker).add(card)
            events += GameEvent.CardAsked(
                askerId = asker, askerName = SEAT_NAMES.getValue(asker),
                targetId = owner, targetName = SEAT_NAMES.getValue(owner),
                card = card, success = true, timestamp = ts++
            )
        }

        // ── Probe phase: failed asks for the hidden card rule out everyone but player_2 ──
        for (opp in TEAM2) {
            events += GameEvent.CardAsked(
                askerId = HUMAN, askerName = SEAT_NAMES.getValue(HUMAN),
                targetId = opp, targetName = SEAT_NAMES.getValue(opp),
                card = hiddenCard, success = false, timestamp = ts++
            )
        }

        // ── Validate from the human's POV with the real deduction oracle ──────
        val players = SEAT_IDS.map { id ->
            Player(id = id, name = SEAT_NAMES.getValue(id), teamId = teamOf(id), hand = hands.getValue(id).toList())
        }
        val tracker = CardTracker().buildState(events, players, HUMAN)

        val trueHolder: Map<Card, String> = buildMap {
            for (id in SEAT_IDS) for (c in hands.getValue(id)) if (c in targetCards) put(c, id)
        }
        if (trueHolder.size != 6) return null
        for (c in targetCards) {
            val known = tracker.knownLocations[c] ?: return null
            if (known != trueHolder[c] || teamOf(known) != HUMAN_TEAM) return null
        }

        // Interesting: the solve isn't just reading the human's own hand.
        if (targetCards.count { it !in hands.getValue("player_0") } < 2) return null

        // Unique: no other unclaimed half-suit is also fully known on team_1.
        val anotherClaimable = HalfSuit.entries.any { hs ->
            hs != target && DeckUtils.getAllCardsForHalfSuit(hs).all { c ->
                tracker.knownLocations[c]?.let { teamOf(it) == HUMAN_TEAM } == true
            }
        }
        if (anotherClaimable) return null

        return DailyPuzzle(
            seed = seed,
            playerCount = PLAYER_COUNT,
            players = players.map { PuzzlePlayer(it.id, it.name, it.teamId, it.hand.size) },
            humanSeatId = HUMAN,
            humanTeamId = HUMAN_TEAM,
            myHand = hands.getValue(HUMAN).sortedWith(compareBy({ it.suit }, { it.value.rank })),
            halfSuitStatuses = HalfSuit.entries.map { HalfSuitStatus(it) },
            events = events,
            answer = PuzzleAnswer(target, targetCards.map { CardHolder(it, trueHolder.getValue(it)) }, hiddenCard)
        )
    }
}
