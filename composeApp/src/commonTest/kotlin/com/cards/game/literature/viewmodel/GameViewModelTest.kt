package com.cards.game.literature.viewmodel

import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.logic.GameEngine
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.ClaimDeclaration
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.GameState
import com.cards.game.literature.repository.GameRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What the board screen is told to draw.
 *
 * `GameViewModel` turns engine state into the thing every player looks at — whose turn it is,
 * which cards are in hand, who sits on which side, what the score is. 385 lines of it, with no
 * tests. The state is built from a real [GameEngine] deal rather than a hand-made fixture, so
 * these tests break if the engine's own shape moves.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    /** Records what was asked of it and lets a test push state, like the real repositories do. */
    private class FakeRepository(initial: GameState? = null) : GameRepository {
        private val _gameState = MutableStateFlow(initial)
        override val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

        private val _gameEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = 16)
        override val gameEvents: Flow<GameEvent> = _gameEvents.asSharedFlow()

        var asked: Triple<String, String, List<Card>>? = null
        var claimed: ClaimDeclaration? = null
        var passedTo: String? = null
        var createdFor: String? = null

        fun push(state: GameState) { _gameState.value = state }
        suspend fun emit(event: GameEvent) = _gameEvents.emit(event)

        override suspend fun createGame(playerName: String, playerCount: Int, difficulty: BotDifficulty): GameState {
            createdFor = playerName
            return _gameState.value ?: error("no state staged")
        }
        override suspend fun submitAsk(askerId: String, targetId: String, card: Card) {
            asked = Triple(askerId, targetId, listOf(card))
        }
        override suspend fun submitMultiAsk(askerId: String, targetId: String, cards: List<Card>) {
            asked = Triple(askerId, targetId, cards)
        }
        override suspend fun submitClaim(declaration: ClaimDeclaration) { claimed = declaration }
        override suspend fun submitPassTarget(selectedPlayerId: String) { passedTo = selectedPlayerId }
    }

    private fun deal(players: Int = 6): GameState =
        GameEngine().createGame("test_game", "Me", players)

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    // ─── the table, as the player sees it ────────────────────────────────────

    @Test
    fun theBoardShowsMyHandAndMyHalfOfTheTable() = runTest(dispatcher) {
        val state = deal()
        val repo = FakeRepository(state)
        val vm = GameViewModel(repo)
        runCurrent()

        val ui = vm.uiState.value
        val me = state.getPlayer("player_0")!!
        assertEquals(me.hand.size, ui.myHand.size, "my hand is mine, not someone else's")
        // A 6-player game is 3 a side: me plus two teammates against three opponents.
        assertEquals(2, ui.teammates.size)
        assertEquals(3, ui.opponents.size)
        assertTrue(ui.opponents.none { it.id == "player_0" }, "I am never my own opponent")
    }

    @Test
    fun theHandIsGroupedByHalfSuitForTheAskSheet() = runTest(dispatcher) {
        val repo = FakeRepository(deal())
        val vm = GameViewModel(repo)
        runCurrent()

        val ui = vm.uiState.value
        assertEquals(ui.myHand.size, ui.myHandByHalfSuit.values.sumOf { it.size },
            "every card in hand appears exactly once in the grouping")
    }

    @Test
    fun whoseTurnItIsFollowsTheEngine() = runTest(dispatcher) {
        val state = deal()
        val repo = FakeRepository(state)
        val vm = GameViewModel(repo)
        runCurrent()

        val ui = vm.uiState.value
        assertEquals(state.currentPlayer.id, ui.activePlayerId)
        assertEquals(state.currentPlayer.name, ui.activePlayerName)
        assertEquals(state.currentPlayer.id == "player_0", ui.isMyTurn)
    }

    @Test
    fun theScoresAreReadFromMySideOfTheTableNotTeamOne() = runTest(dispatcher) {
        // An online player can be on team 2. Reading "my score" as team_1's was a shipped bug.
        val state = deal()
        val repo = FakeRepository(state)
        val vm = GameViewModel(repo, overridePlayerId = "player_1")
        runCurrent()

        val ui = vm.uiState.value
        val myTeam = state.getTeamForPlayer("player_1")
        val theirTeam = state.teams.first { it.id != myTeam?.id }
        assertEquals(myTeam?.score ?: 0, ui.myTeamScore)
        assertEquals(theirTeam.score, ui.opponentTeamScore)
        assertEquals(myTeam?.id, ui.myTeamId)
    }

    @Test
    fun theTurnDeadlineIsCarriedThroughToTheBoard() = runTest(dispatcher) {
        val repo = FakeRepository(deal().copy(turnDeadlineMs = 1_700_000_000_000))
        val vm = GameViewModel(repo)
        runCurrent()

        assertEquals(1_700_000_000_000, vm.uiState.value.turnDeadlineMs)
    }

    @Test
    fun aFinishedGameRunsNoClock() = runTest(dispatcher) {
        val repo = FakeRepository(deal().copy(phase = GamePhase.FINISHED, turnDeadlineMs = null))
        val vm = GameViewModel(repo)
        runCurrent()

        assertEquals(GamePhase.FINISHED, vm.uiState.value.phase)
        assertNull(vm.uiState.value.turnDeadlineMs)
    }

    // ─── what the player's taps actually send ────────────────────────────────

    @Test
    fun askingForSeveralCardsSendsThemAsOneAsk() = runTest(dispatcher) {
        val state = deal()
        val repo = FakeRepository(state)
        val vm = GameViewModel(repo)
        runCurrent()

        val cards = state.getPlayer("player_0")!!.hand.take(2)
        vm.askCards(targetId = "player_1", cards = cards)
        runCurrent()

        // A turn can span several suits deliberately — the queue is one ask, not one per card.
        val (asker, target, sent) = repo.asked!!
        assertEquals("player_0", asker)
        assertEquals("player_1", target)
        assertEquals(cards, sent)
    }

    @Test
    fun selectingAPassTargetForwardsTheChoice() = runTest(dispatcher) {
        val repo = FakeRepository(deal())
        val vm = GameViewModel(repo)
        runCurrent()

        vm.selectPassTarget("player_2")
        runCurrent()

        assertEquals("player_2", repo.passedTo)
    }

    // ─── identity ────────────────────────────────────────────────────────────

    @Test
    fun theSeatOverrideWinsOverTheDefault() = runTest(dispatcher) {
        val repo = FakeRepository(deal())
        val vm = GameViewModel(repo, overridePlayerId = "player_3")
        runCurrent()

        // Online, the server decides which seat is ours; offline it is always player_0.
        assertEquals("player_3", vm.uiState.value.myPlayerId)
        assertFalse(vm.uiState.value.opponents.any { it.id == "player_3" })
    }
}
