package com.cards.game.literature.logic

import com.cards.game.literature.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEngineTest {
    private val engine = GameEngine()

    @Test
    fun createGameWith6Players() {
        val state = engine.createGame("test", "Human", 6)
        assertEquals(6, state.players.size)
        assertEquals(2, state.teams.size)
        assertEquals(GamePhase.IN_PROGRESS, state.phase)
        state.players.forEach { assertEquals(8, it.hand.size) }
    }

    @Test
    fun createGameWith4Players() {
        val state = engine.createGame("test", "Human", 4)
        assertEquals(4, state.players.size)
        state.players.forEach { assertEquals(12, it.hand.size) }
    }

    @Test
    fun successfulAskTransfersCard() {
        val state = engine.createGame("test", "Human", 6)
        val asker = state.currentPlayer
        val opponents = state.getOpponents(asker.id).filter { it.isActive }
        val target = opponents.first()

        // Find a card the target has that's in the same half-suit as something the asker has
        val askerHalfSuits = asker.hand.map { DeckUtils.getHalfSuit(it) }.toSet()
        val targetCard = target.hand.firstOrNull { DeckUtils.getHalfSuit(it) in askerHalfSuits && it !in asker.hand }

        if (targetCard != null) {
            val result = engine.processAsk(state, asker.id, target.id, targetCard)
            val newAsker = result.newState.getPlayer(asker.id)!!
            val newTarget = result.newState.getPlayer(target.id)!!
            assertTrue(targetCard in newAsker.hand)
            assertTrue(targetCard !in newTarget.hand)
        }
    }

    @Test
    fun failedAskPassesTurnToTarget() {
        val state = engine.createGame("test", "Human", 6)
        val asker = state.currentPlayer
        val opponents = state.getOpponents(asker.id).filter { it.isActive }
        val target = opponents.first()

        // Find a card the target does NOT have
        val askerHalfSuits = asker.hand.map { DeckUtils.getHalfSuit(it) }.toSet()
        val missingCard = askerHalfSuits.flatMap { DeckUtils.getAllCardsForHalfSuit(it) }
            .firstOrNull { it !in asker.hand && it !in target.hand }

        if (missingCard != null) {
            val result = engine.processAsk(state, asker.id, target.id, missingCard)
            assertEquals(target.id, result.newState.currentPlayer.id)
        }
    }

    @Test
    fun createMultiplayerGameWith6Players() {
        val players = listOf(
            PlayerSetupInfo("p1", "Alice", "team_1"),
            PlayerSetupInfo("p2", "Bob", "team_2"),
            PlayerSetupInfo("p3", "Charlie", "team_1"),
            PlayerSetupInfo("p4", "Diana", "team_2"),
            PlayerSetupInfo("p5", "Eve", "team_1"),
            PlayerSetupInfo("p6", "Frank", "team_2")
        )
        val state = engine.createMultiplayerGame("test-mp", players)
        assertEquals(6, state.players.size)
        assertEquals(2, state.teams.size)
        assertEquals(GamePhase.IN_PROGRESS, state.phase)
        state.players.forEach { assertEquals(8, it.hand.size) }
        assertEquals("p1", state.players[0].id)
        assertEquals("Alice", state.players[0].name)
    }

    @Test
    fun createGameWith8Players() {
        val state = engine.createGame("test", "Human", 8)
        assertEquals(8, state.players.size)
        assertEquals(2, state.teams.size)
        assertEquals(GamePhase.IN_PROGRESS, state.phase)
        state.players.forEach { assertEquals(6, it.hand.size) }
        state.teams.forEach { assertEquals(4, it.playerIds.size) }
    }

    @Test
    fun createMultiplayerGameWith8Players() {
        val players = (1..8).map { i ->
            PlayerSetupInfo("p$i", "Player $i", if (i % 2 == 1) "team_1" else "team_2")
        }
        val state = engine.createMultiplayerGame("test-mp8", players)
        assertEquals(8, state.players.size)
        state.players.forEach { assertEquals(6, it.hand.size) }
        state.teams.forEach { assertEquals(4, it.playerIds.size) }
        // All 48 cards dealt exactly once
        assertEquals(48, state.players.flatMap { it.hand }.toSet().size)
    }

    @Test
    fun createdGameRecordsDealSeed() {
        val state = engine.createGame("test", "Human", 4)
        // Seed must be recorded so the deal is reproducible
        val redeal = CardDealer.dealCards(4, state.dealSeed)
        val sortedHands = state.players.map { it.hand.toSet() }
        assertEquals(redeal.map { it.toSet() }, sortedHands)
    }
}
