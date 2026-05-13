package com.cards.game.literature.logic

import com.cards.game.literature.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardTrackerTest {
    private val tracker = CardTracker()

    private val s2 = Card(Suit.SPADES, CardValue.TWO)
    private val s3 = Card(Suit.SPADES, CardValue.THREE)
    private val s4 = Card(Suit.SPADES, CardValue.FOUR)
    private val s5 = Card(Suit.SPADES, CardValue.FIVE)
    private val s6 = Card(Suit.SPADES, CardValue.SIX)
    private val s7 = Card(Suit.SPADES, CardValue.SEVEN)

    private fun player(id: String, teamId: String, hand: List<Card> = emptyList()) =
        Player(id = id, name = id, teamId = teamId, hand = hand)

    private fun asked(askerId: String, targetId: String, card: Card, success: Boolean) =
        GameEvent.CardAsked(askerId, askerId, targetId, targetId, card, success)

    @Test
    fun ownHandCardsAreKnown() {
        val players = listOf(player("p1", "t1", listOf(s2, s3)), player("p2", "t2", listOf(s4)))
        val state = tracker.buildState(emptyList(), players, "p1")
        assertEquals("p1", state.knownLocations[s2])
        assertEquals("p1", state.knownLocations[s3])
        assertFalse(state.knownLocations.containsKey(s4))
    }

    @Test
    fun successfulAskMarksCardWithAsker() {
        val players = listOf(player("p1", "t1", listOf(s2, s4)), player("p2", "t2", listOf(s3)))
        val events = listOf(asked("p1", "p2", s4, success = true))
        val state = tracker.buildState(events, players, "p1")
        assertEquals("p1", state.knownLocations[s4])
    }

    @Test
    fun failedAskMarksCardImpossibleForBothParties() {
        val players = listOf(player("p1", "t1", listOf(s2)), player("p2", "t2", listOf(s3)))
        val events = listOf(asked("p1", "p2", s5, success = false))
        val state = tracker.buildState(events, players, "p1")
        val impossible = state.impossibleLocations[s5] ?: emptySet()
        assertTrue("p1" in impossible)
        assertTrue("p2" in impossible)
    }

    @Test
    fun deckClaimedClearsAllCardsInHalfSuitFromTracking() {
        val players = listOf(player("p1", "t1", listOf(s2)), player("p2", "t2", listOf(s3)))
        val events = listOf(
            asked("p1", "p2", s5, success = false),
            GameEvent.DeckClaimed("p1", "p1", "t1", HalfSuit.SPADES_LOW, correct = true)
        )
        val state = tracker.buildState(events, players, "p1")
        DeckUtils.getAllCardsForHalfSuit(HalfSuit.SPADES_LOW).forEach { card ->
            assertFalse(state.knownLocations.containsKey(card))
            assertFalse(state.impossibleLocations.containsKey(card))
        }
    }

    @Test
    fun inactivePlayerCardsRemovedFromKnownLocations() {
        val players = listOf(
            player("p1", "t1", listOf(s2)),
            player("p2", "t2") // inactive — empty hand
        )
        // Event says p2 acquired s3, but p2 is now inactive
        val events = listOf(asked("p2", "p1", s3, success = true))
        val state = tracker.buildState(events, players, "p1")
        assertFalse(state.knownLocations.containsKey(s3))
    }

    @Test
    fun sixthCardDeducedWhenFiveKnownAndOnePlayerRemains() {
        // p1 holds s2–s6; p1 and p2 are ruled out for s7 → only p3 remains → s7 deduced to p3
        val players = listOf(
            player("p1", "t1", listOf(s2, s3, s4, s5, s6)),
            player("p2", "t2", listOf(Card(Suit.HEARTS, CardValue.TWO))),
            player("p3", "t1", listOf(Card(Suit.HEARTS, CardValue.THREE)))
        )
        val events = listOf(asked("p1", "p2", s7, success = false))
        val state = tracker.buildState(events, players, "p1")
        assertEquals("p3", state.knownLocations[s7])
    }

    @Test
    fun sixthCardNotDeducedWhenMultipleCandidatesRemain() {
        val players = listOf(
            player("p1", "t1", listOf(s2, s3, s4, s5, s6)),
            player("p2", "t2", listOf(Card(Suit.HEARTS, CardValue.TWO))),
            player("p3", "t1", listOf(Card(Suit.HEARTS, CardValue.THREE))),
            player("p4", "t2", listOf(Card(Suit.HEARTS, CardValue.FOUR)))
        )
        // No one ruled out for s7 → multiple candidates → no deduction
        val state = tracker.buildState(emptyList(), players, "p1")
        assertFalse(state.knownLocations.containsKey(s7))
    }

    @Test
    fun getKnownCardsForPlayerReturnsOnlyTheirCards() {
        val players = listOf(player("p1", "t1", listOf(s2, s3)), player("p2", "t2", listOf(s4)))
        val state = tracker.buildState(emptyList(), players, "p1")
        val cards = tracker.getKnownCardsForPlayer(state, "p1")
        assertEquals(setOf(s2, s3), cards.toSet())
    }

    @Test
    fun getPossibleHoldersReturnsKnownHolderAlone() {
        val players = listOf(player("p1", "t1", listOf(s2)), player("p2", "t2", listOf(s3)))
        val state = tracker.buildState(emptyList(), players, "p1")
        assertEquals(listOf("p1"), tracker.getPossibleHolders(state, s2, listOf("p1", "p2")))
    }

    @Test
    fun getPossibleHoldersExcludesImpossiblePlayers() {
        val players = listOf(
            player("p1", "t1", listOf(s2)),
            player("p2", "t2", listOf(s3)),
            player("p3", "t1", listOf(s4))
        )
        val events = listOf(asked("p1", "p2", s5, success = false)) // p1 and p2 ruled out for s5
        val state = tracker.buildState(events, players, "p1")
        assertEquals(listOf("p3"), tracker.getPossibleHolders(state, s5, listOf("p1", "p2", "p3")))
    }
}
