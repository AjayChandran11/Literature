package com.cards.game.literature.repository

import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.GamePhase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The offline game — the mode most people actually play, and the one at 7% coverage.
 *
 * This is the loop that deals, applies a move, and then drives every bot in turn until it is a
 * human's go again. A crash anywhere in that loop wedges the board with no server to recover
 * from and no Crashlytics breadcrumb worth reading, which is exactly what F-01 was.
 *
 * Note on ids: `createGame` builds `gameId` from `currentTimeMillis()`, so two deals inside the
 * same millisecond share one. That id is the de-duplication key in `StatsStore.recordGame`, so a
 * collision silently drops a match from the player's stats. Not reachable by tapping — but the
 * tests below deliberately assert on the DEAL rather than the id, so they neither depend on nor
 * bless that behaviour.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalGameRepositoryTest {

    private fun repo() = LocalGameRepository()

    @Test
    fun creatingAGameDealsEveryCardAndSeatsEveryone() = runTest {
        val repo = repo()
        val state = repo.createGame("Me", 6, BotDifficulty.MEDIUM)

        assertEquals(6, state.players.size)
        assertEquals(2, state.teams.size)
        // A Literature deck is 48 cards — the eights are removed — split evenly.
        assertEquals(48, state.players.sumOf { it.hand.size })
        assertTrue(state.players.all { it.hand.size == 8 }, "every seat gets the same number")
        assertEquals(GamePhase.IN_PROGRESS, state.phase)
    }

    @Test
    fun theHumanIsSeatZeroAndEveryoneElseIsABot() = runTest {
        val repo = repo()
        val state = repo.createGame("Me", 6, BotDifficulty.MEDIUM)

        val me = state.getPlayer("player_0")
        assertNotNull(me)
        assertEquals("Me", me.name)
        assertTrue(!me.isBot, "the human is never a bot")
        assertEquals(5, state.players.count { it.isBot })
    }

    @Test
    fun theGameIsPublishedBeforeItIsReturned() = runTest {
        val repo = repo()
        assertNull(repo.gameState.value, "nothing is published before a game exists")

        val state = repo.createGame("Me", 4, BotDifficulty.EASY)

        // The board screen reads the flow, not the return value.
        assertEquals(state.gameId, repo.gameState.value?.gameId)
    }

    @Test
    fun startingAGameAnnouncesIt() = runTest {
        val repo = repo()
        val seen = mutableListOf<GameEvent>()
        backgroundScope.launch { repo.gameEvents.collect { seen += it } }
        runCurrent()

        repo.createGame("Me", 6, BotDifficulty.MEDIUM)
        runCurrent()

        assertTrue(seen.any { it is GameEvent.GameStarted }, "the board is told a match began")
    }

    @Test
    fun aSecondGameReplacesTheFirstRatherThanResumingIt() = runTest {
        val repo = repo()
        val first = repo.createGame("Me", 6, BotDifficulty.MEDIUM)
        val second = repo.createGame("Me", 6, BotDifficulty.MEDIUM)

        // "Play again" deals a genuinely fresh match — not the old one carried forward.
        // (Ids are NOT asserted here on purpose: they are millisecond timestamps and two deals
        // in the same millisecond collide. See the note in the class comment.)
        assertTrue(
            first.players.first().hand != second.players.first().hand,
            "the second game is a new deal"
        )
        val published = repo.gameState.value
        assertEquals(second.players.first().hand, published?.players?.first()?.hand)
    }

    @Test
    fun aMoveIsRejectedWhenThereIsNoGame() = runTest {
        val repo = repo()
        val card = Card(com.cards.game.literature.model.Suit.SPADES, com.cards.game.literature.model.CardValue.TWO)

        // Navigation can restore the board route onto a repository with nothing in it; a tap
        // arriving then must be a no-op rather than an exception.
        repo.submitAsk("player_0", "player_1", card)
        repo.submitMultiAsk("player_0", "player_1", listOf(card))
        repo.submitPassTarget("player_1")

        assertNull(repo.gameState.value)
    }

    @Test
    fun anIllegalAskIsRejectedAndLeavesTheGameUntouched() = runTest {
        val repo = repo()
        val state = repo.createGame("Me", 6, BotDifficulty.MEDIUM)
        val myOwnCard = state.getPlayer("player_0")!!.hand.first()
        val opponent = state.getOpponents("player_0").first()
        val handsBefore = repo.gameState.value!!.players.map { it.hand }

        // Asking for a card you already hold is illegal. The engine throws and the repository
        // lets it through — GameViewModel is the layer that catches and surfaces it — so the
        // contract worth pinning here is that nothing is half-applied on the way out.
        val thrown = runCatching { repo.submitAsk("player_0", opponent.id, myOwnCard) }.exceptionOrNull()

        assertNotNull(thrown, "an illegal ask is rejected, not quietly accepted")
        assertEquals(handsBefore, repo.gameState.value!!.players.map { it.hand }, "no card moved")
    }

    @Test
    fun everyDealtHandGroupsCleanlyIntoHalfSuits() = runTest {
        val repo = repo()
        val state = repo.createGame("Me", 8, BotDifficulty.HARD)

        // The ask sheet groups by half-suit; a card that belongs to none would vanish from it.
        state.players.forEach { player ->
            val grouped = player.hand.groupBy { DeckUtils.getHalfSuit(it) }
            assertEquals(player.hand.size, grouped.values.sumOf { it.size })
        }
    }
}
