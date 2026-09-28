package com.cards.game.literature.viewmodel

import com.cards.game.literature.protocol.RoomPhase
import com.cards.game.literature.protocol.RoomPlayerInfo
import com.cards.game.literature.protocol.RoomState
import com.cards.game.literature.protocol.ServerMessage
import com.cards.game.literature.repository.FatalSessionError
import com.cards.game.literature.repository.OnlineGameRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The lobby — the first screen that ever touches the server, and so the one that meets the
 * version gate and every bad room code.
 *
 * Two shipped bugs lived in exactly this seam: a mistyped code surfaced as a terminal session
 * error instead of an ordinary message, and the lobby never watched `fatalError` at all, so an
 * out-of-date build spun until the admission timeout and then blamed the network.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val wire = Json { classDiscriminator = "type"; encodeDefaults = true }

    private fun repo() = OnlineGameRepository(
        serverUrl = "ws://test.invalid",
        client = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
    )

    private suspend fun OnlineGameRepository.receive(message: ServerMessage) =
        handleServerMessage(wire.encodeToString(ServerMessage.serializer(), message))

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theLobbyStartsIdle() = runTest(dispatcher) {
        val vm = LobbyViewModel(repo())
        runCurrent()

        val ui = vm.uiState.value
        assertFalse(ui.isLoading)
        assertNull(ui.errorMessage)
        assertNull(ui.fatalError)
    }

    @Test
    fun aMistypedCodeIsAnOrdinaryMessage() = runTest(dispatcher) {
        val repo = repo()
        val vm = LobbyViewModel(repo)
        runCurrent()

        // No seat was ever issued, so this is a wrong code rather than a room vanishing.
        repo.roomCode = "ZZZZZZ"
        repo.receive(ServerMessage.Error("Room not found"))
        runCurrent()

        assertEquals("Room not found", vm.uiState.value.errorMessage)
        assertNull(vm.uiState.value.fatalError, "a typo must not read as a dead session")
    }

    @Test
    fun aTerminalSessionErrorReachesTheLobbyAndStopsTheSpinner() = runTest(dispatcher) {
        val repo = repo()
        val vm = LobbyViewModel(repo)
        runCurrent()

        // The bug was that the lobby watched `errors` but never `fatalError`, so anything
        // terminal — the version gate closing the socket on the first connect, a room gone —
        // left the spinner running to the admission timeout and then blamed the network.
        // Driven here with ROOM_GONE because it is reachable through the message pipeline;
        // UPDATE_REQUIRED arrives on a close frame and takes the same path from here on.
        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))
        repo.receive(ServerMessage.Error("Room not found"))
        runCurrent()

        assertEquals(FatalSessionError.ROOM_GONE, vm.uiState.value.fatalError)
        assertFalse(vm.uiState.value.isLoading, "and the spinner stops instead of running to timeout")
    }

    @Test
    fun clearingAnErrorRemovesItFromTheScreen() = runTest(dispatcher) {
        val repo = repo()
        val vm = LobbyViewModel(repo)
        runCurrent()

        repo.receive(ServerMessage.Error("Room is full"))
        runCurrent()
        assertEquals("Room is full", vm.uiState.value.errorMessage)

        vm.clearError()
        runCurrent()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun admissionNavigatesToTheWaitingRoom() = runTest(dispatcher) {
        val repo = repo()
        val vm = LobbyViewModel(repo)
        val destinations = mutableListOf<String>()
        backgroundScope.launch { vm.navigateToWaitingRoom.collect { destinations += it } }
        runCurrent()

        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))
        repo.receive(
            ServerMessage.RoomUpdate(
                RoomState(
                    roomCode = "ABC123",
                    phase = RoomPhase.WAITING,
                    players = listOf(RoomPlayerInfo("player_0", "Me", "team_1", false, true, true)),
                    hostPlayerId = "player_0",
                    targetPlayerCount = 4,
                )
            )
        )
        runCurrent()

        assertTrue(destinations.contains("ABC123"), "being seated is what moves the player on")
        assertFalse(vm.uiState.value.isLoading)
    }
}
