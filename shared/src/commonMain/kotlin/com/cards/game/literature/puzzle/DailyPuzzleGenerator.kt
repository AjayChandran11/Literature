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
 * Produces a [DailyPuzzle] of a chosen [PuzzleKind] deterministically from a seed. The output
 * depends ONLY on (kind, seed) — the deal is seeded via [CardDealer]; every choice draws from a
 * seeded [Random] — so the same calendar day yields the same puzzle on every device with no
 * bundled data and no server. Frozen post-launch behind [PUZZLE_SEASON].
 *
 * Every kind shares one recipe and one fairness guarantee:
 *  1. Deal 4 hands (human = player_0, team_1 = {player_0, player_2}).
 *  2. Build a public ask log — using only *legal, cross-team* asks — that FORCES the answer.
 *  3. Rebuild [CardTracker] from the human's POV (own hand + public log + seat card-counts).
 *  4. Assert the answer is forced from the tracker OUTPUT, and reject ambiguous seeds.
 * [CardTracker] is purely deductive (success→known, fail→impossible, one 5-of-6 inference; no
 * probability), so only answers that are a single provably-forced function of its output are fair.
 */
object DailyPuzzleGenerator {

    /** Bump deliberately if generator heuristics change (keeps "same for everyone" within a version). */
    const val PUZZLE_SEASON = 4

    /**
     * How many OTHER half-suits get decoy moves in the log. Without this, [PuzzleKind.CLAIM]'s
     * Step 1 ("which half-suit can your team claim?") is a giveaway — every logged move would be in
     * the answer's suit. Decoys add truthful, legal asks in unrelated half-suits so the solver must
     * verify. Also the natural difficulty lever for a future ramp: more decoy suits → a noisier log.
     */
    const val DEFAULT_DECOY_SUITS = 2

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

    // Per-kind RNG salts so the same seed yields decorrelated structures across kinds.
    private const val LOCATE_SALT = 0x4C4F43_415445_00L
    private const val WASTED_SALT = 0x574153_544544_00L

    /** Share of LOCATE days that use the (harder) elimination flavour vs the tracked-transfer one. */
    private const val LOCATE_ELIMINATION_PCT = 40

    // ── Public surface ───────────────────────────────────────────────────────

    /** One deterministic attempt for ([kind], [seed]); null if this seed yields no good puzzle. */
    fun generate(kind: PuzzleKind, seed: Long, difficulty: Int = DEFAULT_DECOY_SUITS): DailyPuzzle? =
        when (kind) {
            PuzzleKind.CLAIM -> generateClaim(seed, decoySuits = difficulty)
            PuzzleKind.LOCATE -> generateLocate(seed)
            PuzzleKind.WASTED_ASK -> generateWastedAsk(seed)
        }

    /** First solvable puzzle of [kind] at or after [seed] — the on-device forward-search. */
    fun generateForDay(
        kind: PuzzleKind,
        seed: Long,
        maxAttempts: Int = 300,
        difficulty: Int = DEFAULT_DECOY_SUITS
    ): DailyPuzzle? {
        for (i in 0 until maxAttempts) generate(kind, seed + i, difficulty)?.let { return it }
        return null
    }

    // ── CLAIM: which half-suit can your team claim + the card the teammate hides ──
    //
    // Pick a target half-suit where the teammate (player_2) holds exactly ONE card — that single
    // card becomes the one the solver must deduce. player_2 pulls every target card team_2 holds
    // (visible successful asks), then the human probes both opponents for the hidden card (failed
    // asks) so the teammate is the only candidate. Validate that all six land on team_1.
    private fun generateClaim(seed: Long, decoySuits: Int): DailyPuzzle? {
        val rng = rngFor(seed, 0L)
        val hands = dealHands(seed)

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
        var ts = 0L

        // ── Pull phase: the teammate grabs every target card team_2 holds ──
        var guard = 0
        while (true) {
            val loot = TEAM2.flatMap { o -> hands.getValue(o).filter { it in targetCards }.map { o to it } }
            if (loot.isEmpty()) break
            if (guard++ > 12) return null
            val (owner, card) = loot[rng.nextInt(loot.size)]
            val asker = "player_2"
            hands.getValue(owner).remove(card)
            hands.getValue(asker).add(card)
            events += asked(asker, owner, card, success = true, ts = ts++)
        }

        // ── Probe phase: failed asks for the hidden card rule out everyone but player_2 ──
        for (opp in TEAM2) events += asked(HUMAN, opp, hiddenCard, success = false, ts = ts++)

        // ── Decoy phase: truthful, legal asks in OTHER half-suits ──
        // Prefer a success that moves a card TOWARD team_2 (can't help a team_1 claim), else a
        // failed ask. The uniqueness check below still rejects any decoy that completes a suit.
        var decoysAdded = 0
        for (hs in HalfSuit.entries.filter { it != target }.shuffled(rng)) {
            if (decoysAdded >= decoySuits) break
            val hsCards = DeckUtils.getAllCardsForHalfSuit(hs).toSet()
            var added = false
            for (askerId in TEAM2) {
                val askerHand = hands.getValue(askerId)
                if (askerHand.none { it in hsCards }) continue
                val victim = TEAM1.firstNotNullOfOrNull { t ->
                    hands.getValue(t).firstOrNull { it in hsCards && it !in askerHand }?.let { t to it }
                } ?: continue
                val (targetId, card) = victim
                hands.getValue(targetId).remove(card)
                hands.getValue(askerId).add(card)
                events += asked(askerId, targetId, card, success = true, ts = ts++)
                added = true
                break
            }
            if (!added) {
                for (askerId in SEAT_IDS) {
                    val askerHand = hands.getValue(askerId)
                    if (askerHand.none { it in hsCards }) continue
                    val askCard = hsCards.firstOrNull { it !in askerHand } ?: continue
                    val opponents = if (askerId in TEAM1) TEAM2 else TEAM1
                    val targetId = opponents.firstOrNull { askCard !in hands.getValue(it) } ?: continue
                    events += asked(askerId, targetId, askCard, success = false, ts = ts++)
                    added = true
                    break
                }
            }
            if (added) decoysAdded++
        }
        if (decoySuits > 0 && decoysAdded == 0) return null // never ship a single-suit giveaway

        // ── Validate from the human's POV with the real deduction oracle ──
        val players = buildPlayers(hands)
        val tracker = CardTracker().buildState(events, players, HUMAN)

        val trueHolder: Map<Card, String> = buildMap {
            for (id in SEAT_IDS) for (c in hands.getValue(id)) if (c in targetCards) put(c, id)
        }
        if (trueHolder.size != 6) return null
        for (c in targetCards) {
            val known = tracker.knownLocations[c] ?: return null
            if (known != trueHolder[c] || teamOf(known) != HUMAN_TEAM) return null
        }
        if (targetCards.count { it !in hands.getValue("player_0") } < 2) return null // a real deduction

        val anotherClaimable = HalfSuit.entries.any { hs ->
            hs != target && DeckUtils.getAllCardsForHalfSuit(hs).all { c ->
                tracker.knownLocations[c]?.let { teamOf(it) == HUMAN_TEAM } == true
            }
        }
        if (anotherClaimable) return null

        return puzzle(
            seed, PuzzleKind.CLAIM, hands, events,
            HalfSuitClaim(target, targetCards.map { CardHolder(it, trueHolder.getValue(it)) }, hiddenCard)
        )
    }

    // ── LOCATE: which seat must be holding a named card ──
    //
    // Two flavours, mixed per day so it never feels samey (the answer is always the seat
    // [CardTracker] forces the card to, so both are provably fair):
    //  • ELIMINATION — the card never changes hands; 5 of its half-suit are made known and every
    //    active seat but one is ruled out, so the 6th is *deduced*. (The hard, "no transfer" case.)
    //  • TRANSFER — the card visibly moves via one or more legal successful asks; the answer is
    //    wherever it lands (the last asker). The solver tracks it through the log (sometimes a
    //    single hop, sometimes a chain).
    private fun generateLocate(seed: Long): DailyPuzzle? {
        val rng = rngFor(seed, LOCATE_SALT)
        return if (rng.nextInt(100) < LOCATE_ELIMINATION_PCT) locateByElimination(seed, rng)
        else locateByTransfer(seed, rng)
    }

    private fun locateByElimination(seed: Long, rng: Random): DailyPuzzle? {
        val hands = dealHands(seed)

        data class Cand(val hs: HalfSuit, val seat: String, val card: Card)
        val cands = mutableListOf<Cand>()
        for (hs in HalfSuit.entries) {
            val cards = DeckUtils.getAllCardsForHalfSuit(hs).toSet()
            val p2 = hands.getValue("player_2").count { it in cards }
            val p0 = hands.getValue("player_0").count { it in cards }
            if (p0 < 1) continue // the human must be a legal asker / prober for this suit
            for (seat in listOf("player_1", "player_2", "player_3")) {
                val seatCards = hands.getValue(seat).filter { it in cards }
                if (seatCards.isEmpty()) continue
                if (seat == "player_2") {
                    if (p2 != 1) continue // teammate holds exactly the one deduced card
                } else {
                    if (p2 != 0) continue // teammate's suit cards would be unseen → ambiguous
                    val other = if (seat == "player_1") "player_3" else "player_1"
                    if (hands.getValue(other).none { it in cards }) continue // legal asker to rule out teammate
                }
                seatCards.forEach { cands += Cand(hs, seat, it) }
            }
        }
        if (cands.isEmpty()) return null
        val pick = cands[rng.nextInt(cands.size)]
        val cards = DeckUtils.getAllCardsForHalfSuit(pick.hs).toSet()
        val c = pick.card
        val s = pick.seat

        val events = mutableListOf<GameEvent>()
        var ts = 0L
        if (s == "player_2") {
            for (opp in TEAM2) events += asked(HUMAN, opp, c, success = false, ts = ts++)
        } else {
            val other = if (s == "player_1") "player_3" else "player_1"
            events += asked(other, "player_2", c, success = false, ts = ts++) // rules out other + teammate
            events += asked(HUMAN, other, c, success = false, ts = ts++)       // rules out human + other
        }
        // Pull every non-C suit card team_2 holds to the human (seen → known). After this, the only
        // unknown card of the suit is C, forced to S. Order after the failed asks is fine (tracker
        // aggregates), and at ask-time the failed-ask askers above still held a suit card.
        for (opp in TEAM2) {
            for (card in hands.getValue(opp).filter { it in cards && it != c }.toList()) {
                hands.getValue(opp).remove(card)
                hands.getValue(HUMAN).add(card)
                events += asked(HUMAN, opp, card, success = true, ts = ts++)
            }
        }
        ts = addDecoys(hands, events, rng, exclude = setOf(pick.hs), max = DEFAULT_DECOY_SUITS, startTs = ts)

        val players = buildPlayers(hands)
        val tracker = CardTracker().buildState(events, players, HUMAN)
        if (c in hands.getValue(HUMAN)) return null
        if (tracker.knownLocations[c] != s) return null
        if (events.any { it is GameEvent.CardAsked && it.success && it.card == c }) return null // genuine deduction

        return puzzle(seed, PuzzleKind.LOCATE, hands, events, LocateCard(c, s))
    }

    /**
     * "Tracked transfer" flavour: move one card via 1..3 legal cross-team successful asks to a
     * non-human seat; the answer is where it lands (the last asker). Each hop needs the asker to
     * hold a suit-mate of the card (so the ask is legal) and to be on the other team from the
     * current holder. The immediate previous holder is skipped to avoid an ugly take-back.
     */
    private fun locateByTransfer(seed: Long, rng: Random): DailyPuzzle? {
        val baseHands = dealHands(seed)
        val targetLen = 1 + rng.nextInt(3) // aim for 1..3 hops; settle for fewer if the chain stalls
        for (c in DeckUtils.createFullDeck().shuffled(rng)) {
            val hands = baseHands.mapValues { it.value.toMutableList() }
            val hsCards = DeckUtils.getAllCardsForHalfSuit(DeckUtils.getHalfSuit(c)).toSet()
            val events = mutableListOf<GameEvent>()
            var ts = 0L
            var holder = SEAT_IDS.first { c in hands.getValue(it) }
            var prevHolder: String? = null
            var moves = 0
            while (moves < targetLen) {
                val askers = SEAT_IDS.filter {
                    it != holder && it != prevHolder &&
                        teamOf(it) != teamOf(holder) &&
                        c !in hands.getValue(it) &&
                        hands.getValue(it).any { card -> card in hsCards && card != c }
                }
                if (askers.isEmpty()) break
                val asker = askers[rng.nextInt(askers.size)]
                hands.getValue(holder).remove(c)
                hands.getValue(asker).add(c)
                events += asked(asker, holder, c, success = true, ts = ts++)
                prevHolder = holder
                holder = asker
                moves++
            }
            if (moves == 0 || holder == HUMAN) continue // need a visible move, ending off your own hand
            ts = addDecoys(
                hands, events, rng,
                exclude = setOf(DeckUtils.getHalfSuit(c)), max = DEFAULT_DECOY_SUITS, startTs = ts
            )

            val players = buildPlayers(hands)
            val tracker = CardTracker().buildState(events, players, HUMAN)
            if (c in hands.getValue(HUMAN)) continue
            if (tracker.knownLocations[c] != holder) continue // answer = where the log lands it
            return puzzle(seed, PuzzleKind.LOCATE, hands, events, LocateCard(c, holder))
        }
        return null
    }

    // ── WASTED_ASK: which opponent is provably unable to hold a card you'd ask for ──
    //
    // Pick a card C the human could legally ask for (holds a suit-mate, not C). One legal failed ask
    // for C against opponent R rules R out (and stays uncertain everywhere else); the other opponent
    // is never ruled out, so it's a genuine "could still have it" distractor. C must stay unknown —
    // keep <5 of its suit known so the 5-of-6 inference can't fire.
    private fun generateWastedAsk(seed: Long): DailyPuzzle? {
        val rng = rngFor(seed, WASTED_SALT)
        val hands = dealHands(seed)
        val humanHand = hands.getValue(HUMAN)

        data class Cand(val card: Card, val ruledOut: String, val asker: String)
        val cands = mutableListOf<Cand>()
        for (r in TEAM2) {
            for (c in DeckUtils.createFullDeck()) {
                if (c in humanHand) continue // can't ask for a card you hold
                val hsCards = DeckUtils.getAllCardsForHalfSuit(DeckUtils.getHalfSuit(c)).toSet()
                val p0InHs = humanHand.count { it in hsCards }
                if (p0InHs < 1 || p0InHs > 4) continue // legal asker, but <5 known so C stays unknown
                if (c in hands.getValue(r)) continue   // R must truly lack C (a truthful failed ask)
                // Prefer the teammate as asker when legal (reads as a less trivial deduction).
                val p2 = hands.getValue("player_2")
                val asker = if (p2.any { it in hsCards } && c !in p2) "player_2" else HUMAN
                cands += Cand(c, r, asker)
            }
        }
        if (cands.isEmpty()) return null
        val pick = cands[rng.nextInt(cands.size)]

        val events = mutableListOf<GameEvent>()
        var ts = 0L
        events += asked(pick.asker, pick.ruledOut, pick.card, success = false, ts = ts++)
        // Liven the log with a couple of cards visibly changing hands in OTHER suits (a Wasted-Ask
        // log is otherwise all misses, which reads dead), then one failed ask for texture. Both stay
        // out of the answer's suit, so they can't disturb the deduction; the oracle re-checks below.
        val exclude = setOf(DeckUtils.getHalfSuit(pick.card))
        ts = addTransferDecoys(hands, events, rng, exclude = exclude, maxSuits = 2, startTs = ts)
        ts = addDecoys(hands, events, rng, exclude = exclude, max = 1, startTs = ts)

        val players = buildPlayers(hands)
        val tracker = CardTracker().buildState(events, players, HUMAN)
        val hsCards = DeckUtils.getAllCardsForHalfSuit(DeckUtils.getHalfSuit(pick.card)).toSet()
        if (tracker.knownLocations[pick.card] != null) return null // location must stay uncertain
        val ruledOutOpps = TEAM2.filter { it in (tracker.impossibleLocations[pick.card] ?: emptySet()) }
        if (ruledOutOpps != listOf(pick.ruledOut)) return null // exactly one opponent provably out
        if (humanHand.count { it in hsCards } < 1 || pick.card in humanHand) return null // legal ask

        return puzzle(seed, PuzzleKind.WASTED_ASK, hands, events, WastedAsk(pick.card, pick.ruledOut))
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private fun rngFor(seed: Long, salt: Long): Random =
        Random(seed xor (PUZZLE_SEASON.toLong() * -0x61c8864680b583ebL) xor salt)

    private fun dealHands(seed: Long): Map<String, MutableList<Card>> {
        val dealt = CardDealer.dealCards(PLAYER_COUNT, seed)
        return SEAT_IDS.mapIndexed { i, id -> id to dealt[i].toMutableList() }.toMap()
    }

    private fun buildPlayers(hands: Map<String, MutableList<Card>>): List<Player> =
        SEAT_IDS.map { id ->
            Player(id = id, name = SEAT_NAMES.getValue(id), teamId = teamOf(id), hand = hands.getValue(id).toList())
        }

    private fun asked(asker: String, target: String, card: Card, success: Boolean, ts: Long): GameEvent.CardAsked =
        GameEvent.CardAsked(
            askerId = asker, askerName = SEAT_NAMES.getValue(asker),
            targetId = target, targetName = SEAT_NAMES.getValue(target),
            card = card, success = success, timestamp = ts
        )

    /**
     * Add up to [max] truthful, legal *failed* asks in half-suits outside [exclude], for texture so
     * the log isn't a single suit. Failed asks move no card, so they never disturb the answer's
     * deduction (which lives in an excluded suit). Read-only on [hands].
     */
    private fun addDecoys(
        hands: Map<String, MutableList<Card>>,
        events: MutableList<GameEvent>,
        rng: Random,
        exclude: Set<HalfSuit>,
        max: Int,
        startTs: Long
    ): Long {
        var ts = startTs
        var added = 0
        for (hs in HalfSuit.entries.filter { it !in exclude }.shuffled(rng)) {
            if (added >= max) break
            val hsCards = DeckUtils.getAllCardsForHalfSuit(hs).toSet()
            for (askerId in SEAT_IDS.shuffled(rng)) {
                val askerHand = hands.getValue(askerId)
                if (askerHand.none { it in hsCards }) continue
                val askCard = hsCards.firstOrNull { it !in askerHand } ?: continue
                val targets = if (askerId in TEAM1) TEAM2 else TEAM1
                val targetId = targets.firstOrNull { askCard !in hands.getValue(it) } ?: continue
                events += asked(askerId, targetId, askCard, success = false, ts = ts++)
                added++
                break
            }
        }
        return ts
    }

    /**
     * Add up to [maxSuits] truthful, legal *successful* asks in half-suits outside [exclude] — cards
     * visibly changing hands between seats — so a log that would otherwise be all misses reads like a
     * live game. One transfer per suit: an asker that holds a suit-mate (a legal ask) pulls a card it
     * lacks from an opposite-team holder. Confined to excluded suits so it never disturbs the answer's
     * deduction (which lives in an excluded suit); mutates [hands] to reflect each move.
     */
    private fun addTransferDecoys(
        hands: Map<String, MutableList<Card>>,
        events: MutableList<GameEvent>,
        rng: Random,
        exclude: Set<HalfSuit>,
        maxSuits: Int,
        startTs: Long
    ): Long {
        var ts = startTs
        var added = 0
        for (hs in HalfSuit.entries.filter { it !in exclude }.shuffled(rng)) {
            if (added >= maxSuits) break
            val hsCards = DeckUtils.getAllCardsForHalfSuit(hs).toSet()
            for (askerId in SEAT_IDS.shuffled(rng)) {
                val askerHand = hands.getValue(askerId)
                if (askerHand.none { it in hsCards }) continue // asker must hold a suit-mate to ask
                val loot = SEAT_IDS.firstNotNullOfOrNull { holderId ->
                    if (teamOf(holderId) == teamOf(askerId)) return@firstNotNullOfOrNull null
                    hands.getValue(holderId).firstOrNull { it in hsCards && it !in askerHand }
                        ?.let { holderId to it }
                } ?: continue
                val (holderId, card) = loot
                hands.getValue(holderId).remove(card)
                hands.getValue(askerId).add(card)
                events += asked(askerId, holderId, card, success = true, ts = ts++)
                added++
                break
            }
        }
        return ts
    }

    private fun puzzle(
        seed: Long,
        kind: PuzzleKind,
        hands: Map<String, MutableList<Card>>,
        events: List<GameEvent>,
        answer: PuzzleAnswer
    ): DailyPuzzle {
        val players = buildPlayers(hands)
        return DailyPuzzle(
            seed = seed,
            kind = kind,
            playerCount = PLAYER_COUNT,
            players = players.map { PuzzlePlayer(it.id, it.name, it.teamId, it.hand.size) },
            humanSeatId = HUMAN,
            humanTeamId = HUMAN_TEAM,
            myHand = hands.getValue(HUMAN).sortedWith(compareBy({ it.suit }, { it.value.rank })),
            halfSuitStatuses = HalfSuit.entries.map { HalfSuitStatus(it) },
            events = events,
            answer = answer
        )
    }
}
