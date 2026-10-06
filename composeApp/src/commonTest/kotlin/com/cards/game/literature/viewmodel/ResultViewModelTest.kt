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
import kotlin.test.assertTrue

/**
 * The scoreboard at the end of a match.
 *
 * Getting this wrong is uniquely bad: it is the last thing a player sees, and an online player
 * can be seated on either team. Reading "my score" as team 1's shipped once and told half the
 * table they had lost a game they won.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ResultViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FinishedRepository(private val state: GameState) : GameRepository {
        private val _gameState = MutableStateFlow<GameState?>(state)
        override val gameState: StateFlow<GameState?> = _gameState.asStateFlow()
        private val _gameEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
        override val gameEvents: Flow<GameEvent> = _gameEvents.asSharedFlow()
        override suspend fun createGame(playerName: String, playerCount: Int, difficulty: BotDifficulty) = state
        override suspend fun submitAsk(askerId: String, targetId: String, card: Card) = Unit
        override suspend fun submitMultiAsk(askerId: String, targetId: String, cards: List<Card>) = Unit
        override suspend fun submitClaim(declaration: ClaimDeclaration) = Unit
        override suspend fun submitPassTarget(selectedPlayerId: String) = Unit
    }

    /** A finished game with an explicit score on each side. */
    private fun finished(teamOne: Int, teamTwo: Int): GameState {
        val base = GameEngine().createGame("done", "Me", 6)
        return base.copy(
            phase = GamePhase.FINISHED,
            teams = base.teams.map {
                it.copy(score = if (it.id == "team_1") teamOne else teamTwo)
            }
        )
    }

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aWinForMySideReadsAsAWin() = runTest(dispatcher) {
        // player_0 is on team_1.
        val vm = ResultViewModel(FinishedRepository(finished(teamOne = 6, teamTwo = 2)), "player_0")
        runCurrent()

        val ui = vm.uiState.value
        assertEquals(6, ui.myTeamScore)
        assertEquals(2, ui.opponentTeamScore)
        assertTrue(ui.isWinner)
        assertFalse(ui.isDraw)
    }

    @Test
    fun theSameGameReadsAsALossFromTheOtherSideOfTheTable() = runTest(dispatcher) {
        // player_1 is on team_2, and this is the bug that shipped: the loser's screen must not
        // be built from team_1's numbers just because team_1 is first in the list.
        val vm = ResultViewModel(FinishedRepository(finished(teamOne = 6, teamTwo = 2)), "player_1")
        runCurrent()

        val ui = vm.uiState.value
        assertEquals(2, ui.myTeamScore, "my score is my own team's")
        assertEquals(6, ui.opponentTeamScore)
        assertFalse(ui.isWinner)
        assertEquals("team_2", ui.myTeamId)
    }

    @Test
    fun anEvenScoreIsADrawForBothSides() = runTest(dispatcher) {
        val forMe = ResultViewModel(FinishedRepository(finished(4, 4)), "player_0")
        val forThem = ResultViewModel(FinishedRepository(finished(4, 4)), "player_1")
        runCurrent()

        assertTrue(forMe.uiState.value.isDraw)
        assertTrue(forThem.uiState.value.isDraw)
        assertFalse(forMe.uiState.value.isWinner)
        assertFalse(forThem.uiState.value.isWinner)
    }

    @Test
    fun anOfflineResultOffersNoRematch() = runTest(dispatcher) {
        // Rematch resets a server room; there is nothing to reset in a local game, and the
        // button used to be offered anyway.
        val vm = ResultViewModel(FinishedRepository(finished(6, 2)), "player_0")
        runCurrent()

        assertFalse(vm.uiState.value.canRematch)
        assertEquals("", vm.roomCode)
    }

    @Test
    fun anOfflineResultIsNotTreatedAsAnOnlineOne() = runTest(dispatcher) {
        // isOnline is what decides whether the primary button is Play Again or a "waiting for
        // the host" line. Getting it wrong offline would leave a local game with no way to
        // start another one.
        val vm = ResultViewModel(FinishedRepository(finished(6, 2)), "player_0")
        runCurrent()

        assertFalse(vm.uiState.value.isOnline)
        assertFalse(vm.uiState.value.canRematch)
        assertEquals("", vm.uiState.value.hostName)
    }

    @Test
    fun theHalfSuitBreakdownIsCarriedToTheResultScreen() = runTest(dispatcher) {
        val state = finished(6, 2)
        val vm = ResultViewModel(FinishedRepository(state), "player_0")
        runCurrent()

        assertEquals(state.halfSuitStatuses.size, vm.uiState.value.halfSuitBreakdown.size)
    }

    @Test
    fun anUnfinishedGameProducesNoResult() = runTest(dispatcher) {
        // Reaching this screen without a finished game means something went wrong upstream;
        // it must not invent a 0-0 loss.
        val running = GameEngine().createGame("running", "Me", 6)
        val vm = ResultViewModel(FinishedRepository(running), "player_0")
        runCurrent()

        val ui = vm.uiState.value
        assertEquals(0, ui.myTeamScore)
        assertEquals("", ui.myTeamId)
        assertFalse(ui.isWinner)
    }
}
