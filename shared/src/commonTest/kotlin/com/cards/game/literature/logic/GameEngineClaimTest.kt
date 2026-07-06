package com.cards.game.literature.logic

import com.cards.game.literature.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for GameEngine.processClaim and early-game-end handling.
 * States are built by hand so every card location is deterministic.
 *
 * Layout used throughout: 4 players, t1 = (p1, p3), t2 = (p2, p4),
 * seating order p1, p2, p3, p4. p1 is the current player.
 */
class GameEngineClaimTest {
    private val engine = GameEngine()

    // 2,3,4,5,6,7 of spades — in lowValues order
    private val spadesLow = DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW)
    private val first3 = spadesLow.take(3)
    private val last3 = spadesLow.drop(3)

    private val heartsAce = Card(Suit.HEARTS, CardValue.ACE)
    private val heartsTwo = Card(Suit.HEARTS, CardValue.TWO)
    private val heartsThree = Card(Suit.HEARTS, CardValue.THREE)

    private fun buildState(
        p1Hand: List<Card>,
        p2Hand: List<Card>,
        p3Hand: List<Card>,
        p4Hand: List<Card>,
        halfSuitStatuses: List<HalfSuitStatus> = HalfSuit.entries.map { HalfSuitStatus(it) },
        team1Score: Int = 0,
        team2Score: Int = 0
    ): GameState {
        val players = listOf(
            Player("p1", "Player 1", "t1", p1Hand),
            Player("p2", "Player 2", "t2", p2Hand),
            Player("p3", "Player 3", "t1", p3Hand),
            Player("p4", "Player 4", "t2", p4Hand)
        )
        val teams = listOf(
            Team("t1", "Team 1", listOf("p1", "p3"), score = team1Score),
            Team("t2", "Team 2", listOf("p2", "p4"), score = team2Score)
        )
        return GameState(
            gameId = "claim-test",
            players = players,
            teams = teams,
            currentPlayerIndex = 0,
            phase = GamePhase.IN_PROGRESS,
            halfSuitStatuses = halfSuitStatuses,
            playerCount = 4
        )
    }

    private fun claim(assignments: Map<String, List<Card>>) =
        ClaimDeclaration("p1", HalfSuit.SPADES_LOW, assignments)

    @Test
    fun correctClaimAwardsPointAndRemovesCards() {
        val state = buildState(
            p1Hand = first3 + heartsAce, // extra card keeps claimer active
            p2Hand = listOf(heartsTwo),
            p3Hand = last3,
            p4Hand = listOf(heartsThree)
        )
        val result = engine.processClaim(state, claim(mapOf("p1" to first3, "p3" to last3)))
        val newState = result.newState

        assertEquals(1, newState.getTeam("t1")!!.score)
        assertEquals(0, newState.getTeam("t2")!!.score)

        val status = newState.halfSuitStatuses.first { it.halfSuit == HalfSuit.SPADES_LOW }
        assertEquals("t1", status.claimedByTeamId)
        assertEquals(true, status.claimCorrect)

        // Claimed cards removed from every hand
        assertEquals(listOf(heartsAce), newState.getPlayer("p1")!!.hand)
        assertTrue(newState.getPlayer("p3")!!.hand.isEmpty())

        // Claimer still active — keeps the turn
        assertEquals("p1", newState.currentPlayer.id)

        val claimEvent = result.events.filterIsInstance<GameEvent.DeckClaimed>().single()
        assertTrue(claimEvent.correct)
        assertEquals("t1", claimEvent.teamId)
    }

    @Test
    fun wrongAssignmentBetweenTeammatesAwardsPointToOpponents() {
        val state = buildState(
            p1Hand = first3 + heartsAce,
            p2Hand = listOf(heartsTwo),
            p3Hand = last3,
            p4Hand = listOf(heartsThree)
        )
        // Team holds all 6 cards, but two are assigned to the wrong teammate
        val swapped = mapOf(
            "p1" to listOf(first3[0], first3[1], last3[0]),
            "p3" to listOf(first3[2], last3[1], last3[2])
        )
        val result = engine.processClaim(state, claim(swapped))
        val newState = result.newState

        assertEquals(0, newState.getTeam("t1")!!.score)
        assertEquals(1, newState.getTeam("t2")!!.score)

        val status = newState.halfSuitStatuses.first { it.halfSuit == HalfSuit.SPADES_LOW }
        assertEquals("t2", status.claimedByTeamId)
        assertEquals(false, status.claimCorrect)

        // Cards still removed from all hands on a wrong claim
        assertEquals(listOf(heartsAce), newState.getPlayer("p1")!!.hand)
        assertTrue(newState.getPlayer("p3")!!.hand.isEmpty())

        // Turn passes to the first active opponent
        assertEquals("p2", newState.currentPlayer.id)
    }

    @Test
    fun claimIsWrongWhenOpponentHoldsACard() {
        val state = buildState(
            p1Hand = first3 + heartsAce,
            p2Hand = listOf(last3[2], heartsTwo), // opponent secretly holds 7♠
            p3Hand = last3.take(2),
            p4Hand = listOf(heartsThree)
        )
        val result = engine.processClaim(
            state, claim(mapOf("p1" to first3, "p3" to last3))
        )
        val newState = result.newState

        assertEquals(1, newState.getTeam("t2")!!.score)
        assertFalse(newState.halfSuitStatuses.first { it.halfSuit == HalfSuit.SPADES_LOW }.claimCorrect!!)
        // Opponent's claimed card removed too
        assertEquals(listOf(heartsTwo), newState.getPlayer("p2")!!.hand)
    }

    @Test
    fun claimerOutOfCardsPassesTurnToFirstActiveTeammate() {
        val state = buildState(
            p1Hand = first3, // claim consumes claimer's whole hand
            p2Hand = listOf(heartsTwo),
            p3Hand = last3 + heartsAce, // teammate stays active
            p4Hand = listOf(heartsThree)
        )
        val result = engine.processClaim(state, claim(mapOf("p1" to first3, "p3" to last3)))
        val newState = result.newState

        assertEquals(1, newState.getTeam("t1")!!.score)
        // Deterministic pass to first active teammate, not random
        assertEquals("p3", newState.currentPlayer.id)
        assertEquals(GamePhase.IN_PROGRESS, newState.phase)
    }

    @Test
    fun claimCompletingAllHalfSuitsFinishesGame() {
        // 7 half-suits already claimed: 4 by t1, 3 by t2 (SPADES_LOW is enum
        // index 0, so odd indices = 4 suits go to t1 → t1 wins outright after
        // claiming the last one; an even split would end 4-4, a draw)
        val preClaimed = HalfSuit.entries.mapIndexed { i, hs ->
            if (hs == HalfSuit.SPADES_LOW) HalfSuitStatus(hs)
            else HalfSuitStatus(hs, claimedByTeamId = if (i % 2 == 1) "t1" else "t2", claimCorrect = true)
        }
        val t1Pre = preClaimed.count { it.claimedByTeamId == "t1" }
        val t2Pre = preClaimed.count { it.claimedByTeamId == "t2" }

        val state = buildState(
            p1Hand = first3 + heartsAce,
            p2Hand = listOf(heartsTwo),
            p3Hand = last3,
            p4Hand = listOf(heartsThree),
            halfSuitStatuses = preClaimed,
            team1Score = t1Pre,
            team2Score = t2Pre
        )
        val result = engine.processClaim(state, claim(mapOf("p1" to first3, "p3" to last3)))
        val newState = result.newState

        assertEquals(GamePhase.FINISHED, newState.phase)
        assertEquals(t1Pre + 1, newState.getTeam("t1")!!.score)
        assertEquals(t2Pre, newState.getTeam("t2")!!.score)
        assertTrue(newState.halfSuitStatuses.all { it.claimedByTeamId != null })

        val ended = newState.events.filterIsInstance<GameEvent.GameEnded>().last()
        assertEquals("t1", ended.winnerTeamId)
    }

    @Test
    fun teamEmptiedByOwnClaimGivesUnclaimedSuitsToOpponents() {
        // t1's entire holding is the claimed half-suit — winning the claim
        // empties the team, so the other 7 suits go to t2 as bonus
        val state = buildState(
            p1Hand = first3,
            p2Hand = listOf(heartsTwo),
            p3Hand = last3,
            p4Hand = listOf(heartsThree)
        )
        val result = engine.processClaim(state, claim(mapOf("p1" to first3, "p3" to last3)))
        val newState = result.newState

        assertEquals(GamePhase.FINISHED, newState.phase)
        assertEquals(1, newState.getTeam("t1")!!.score)
        assertEquals(7, newState.getTeam("t2")!!.score)
        assertTrue(newState.halfSuitStatuses.all { it.claimedByTeamId != null })

        val ended = newState.events.filterIsInstance<GameEvent.GameEnded>().last()
        assertEquals("t2", ended.winnerTeamId)
    }

    // ---- Option C: claimer picks who continues after emptying their hand ----
    //
    // Uses a 6-player layout so the claimer's team has TWO other teammates left,
    // t1 = (p1, p3, p5), t2 = (p2, p4, p6), seating p1..p6, p1 is current.
    // The 6 SPADES_LOW cards are split across p1/p3/p5 so a correct claim empties
    // p1 while p3 and p5 stay active (each keeps a spare card).

    private val clubTwo = Card(Suit.CLUBS, CardValue.TWO)
    private val clubThree = Card(Suit.CLUBS, CardValue.THREE)
    private val clubFour = Card(Suit.CLUBS, CardValue.FOUR)

    private fun buildSixPlayerState(
        p1Hand: List<Card>,
        p3Hand: List<Card>,
        p5Hand: List<Card>,
        p2Hand: List<Card> = listOf(clubTwo),
        p4Hand: List<Card> = listOf(clubThree),
        p6Hand: List<Card> = listOf(clubFour)
    ): GameState {
        val players = listOf(
            Player("p1", "Player 1", "t1", p1Hand),
            Player("p2", "Player 2", "t2", p2Hand),
            Player("p3", "Player 3", "t1", p3Hand),
            Player("p4", "Player 4", "t2", p4Hand),
            Player("p5", "Player 5", "t1", p5Hand),
            Player("p6", "Player 6", "t2", p6Hand)
        )
        val teams = listOf(
            Team("t1", "Team 1", listOf("p1", "p3", "p5")),
            Team("t2", "Team 2", listOf("p2", "p4", "p6"))
        )
        return GameState(
            gameId = "optionc-test",
            players = players,
            teams = teams,
            currentPlayerIndex = 0,
            phase = GamePhase.IN_PROGRESS,
            playerCount = 6
        )
    }

    // p1 holds 2, p3 & p5 hold 2 each (+ a spare) — correct claim empties only p1.
    private fun buildTwoEligibleState() = buildSixPlayerState(
        p1Hand = spadesLow.take(2),
        p3Hand = spadesLow.subList(2, 4) + heartsTwo,
        p5Hand = spadesLow.subList(4, 6) + heartsThree
    )

    private val twoEligibleClaim = ClaimDeclaration(
        "p1", HalfSuit.SPADES_LOW,
        mapOf(
            "p1" to spadesLow.take(2),
            "p3" to spadesLow.subList(2, 4),
            "p5" to spadesLow.subList(4, 6)
        )
    )

    @Test
    fun pauseFlagSuspendsForSelectionWhenTwoTeammatesActive() {
        val result = engine.processClaim(buildTwoEligibleState(), twoEligibleClaim, pauseForPassSelection = true)
        val newState = result.newState

        // Claim resolved (point awarded, cards removed) but the turn is NOT moved.
        assertEquals(1, newState.getTeam("t1")!!.score)
        assertEquals(GamePhase.IN_PROGRESS, newState.phase)
        assertTrue(newState.isAwaitingPassSelection)
        assertEquals("p1", newState.currentPlayer.id) // still on the claimer until they pick

        val pending = newState.pendingPass!!
        assertEquals("p1", pending.claimerId)
        assertEquals(listOf("p3", "p5"), pending.eligibleTeammateIds) // seating order
        assertEquals("p3", pending.defaultTarget)

        // DeckClaimed happened; TurnChanged is deferred until resolution.
        assertEquals(1, result.events.filterIsInstance<GameEvent.DeckClaimed>().size)
        assertTrue(result.events.none { it is GameEvent.TurnChanged })
    }

    @Test
    fun applyPassSelectionMovesTurnToChosenTeammate() {
        val paused = engine.processClaim(buildTwoEligibleState(), twoEligibleClaim, pauseForPassSelection = true).newState

        val resolved = engine.applyPassSelection(paused, "p5") // pick the non-default teammate
        val newState = resolved.newState

        assertEquals("p5", newState.currentPlayer.id)
        assertNull(newState.pendingPass)
        assertFalse(newState.isAwaitingPassSelection)
        val turn = resolved.events.filterIsInstance<GameEvent.TurnChanged>().single()
        assertEquals("p5", turn.newPlayerId)
    }

    @Test
    fun defaultTargetReproducesLegacyFirstTeammatePass() {
        val paused = engine.processClaim(buildTwoEligibleState(), twoEligibleClaim, pauseForPassSelection = true).newState

        // The timeout/disconnect/bot fallback uses defaultTarget == first active teammate.
        val resolved = engine.applyPassSelection(paused, paused.pendingPass!!.defaultTarget)
        assertEquals("p3", resolved.newState.currentPlayer.id)
    }

    @Test
    fun applyPassSelectionRejectsIneligibleTargets() {
        val paused = engine.processClaim(buildTwoEligibleState(), twoEligibleClaim, pauseForPassSelection = true).newState

        assertFailsWith<IllegalArgumentException> { engine.applyPassSelection(paused, "p2") } // opponent
        assertFailsWith<IllegalArgumentException> { engine.applyPassSelection(paused, "p1") } // claimer himself
    }

    @Test
    fun applyPassSelectionRequiresAPendingPass() {
        val plain = buildTwoEligibleState() // nothing pending
        assertFailsWith<IllegalArgumentException> { engine.applyPassSelection(plain, "p3") }
    }

    @Test
    fun pauseFlagOffKeepsDeterministicAutoPass() {
        // Same scenario, flag defaulted off → behaves exactly as before Option C.
        val result = engine.processClaim(buildTwoEligibleState(), twoEligibleClaim)
        val newState = result.newState

        assertNull(newState.pendingPass)
        assertEquals("p3", newState.currentPlayer.id) // first active teammate
        assertTrue(result.events.any { it is GameEvent.TurnChanged })
    }

    @Test
    fun noPauseWhenOnlyOneTeammateRemainsActive() {
        // p5 holds only claimed cards, so after the claim p3 is the sole eligible
        // teammate — no real choice, so no suspension even with the flag on.
        val state = buildSixPlayerState(
            p1Hand = spadesLow.take(2),
            p3Hand = spadesLow.subList(2, 4) + heartsTwo, // stays active
            p5Hand = spadesLow.subList(4, 6)              // empties with the claim
        )
        val result = engine.processClaim(state, twoEligibleClaim, pauseForPassSelection = true)

        assertNull(result.newState.pendingPass)
        assertEquals("p3", result.newState.currentPlayer.id)
    }

    @Test
    fun gameEndingClaimNeverPausesEvenWithFlagOn() {
        // Whole team's holding is the claimed suit → claim empties t1 and ends the
        // game; game-over must win over the pause path.
        val state = buildSixPlayerState(
            p1Hand = spadesLow.take(2),
            p3Hand = spadesLow.subList(2, 4),
            p5Hand = spadesLow.subList(4, 6)
        )
        val result = engine.processClaim(state, twoEligibleClaim, pauseForPassSelection = true)

        assertEquals(GamePhase.FINISHED, result.newState.phase)
        assertNull(result.newState.pendingPass)
    }

    @Test
    fun askEmptyingOpposingTeamEndsGameWithBonusPoints() {
        // p2 holds t2's last card; p1 takes it with a successful ask
        val spadesFour = spadesLow[2]
        val state = buildState(
            p1Hand = listOf(spadesLow[0], spadesLow[1]),
            p2Hand = listOf(spadesFour),
            p3Hand = listOf(heartsTwo),
            p4Hand = emptyList()
        )
        val result = engine.processAsk(state, "p1", "p2", spadesFour)
        val newState = result.newState

        assertEquals(GamePhase.FINISHED, newState.phase)
        // All 8 unclaimed half-suits awarded to t1
        assertEquals(8, newState.getTeam("t1")!!.score)
        assertEquals(0, newState.getTeam("t2")!!.score)
        assertTrue(newState.halfSuitStatuses.all { it.claimedByTeamId == "t1" })

        val ended = newState.events.filterIsInstance<GameEvent.GameEnded>().last()
        assertEquals("t1", ended.winnerTeamId)
    }
}
