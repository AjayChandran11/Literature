package com.cards.game.literature.viewmodel

import com.cards.game.literature.protocol.RoomPhase
import com.cards.game.literature.protocol.RoomPlayerInfo
import com.cards.game.literature.protocol.RoomState
import com.cards.game.literature.protocol.ServerMessage
import com.cards.game.literature.repository.FatalSessionError
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.repository.PlayerConnectionEvent
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
 * The waiting room, driven by real server frames.
 *
 * These go through the repository rather than around it: a frame is encoded exactly as the server
 * would send it, handed to the live message pipeline, and the assertion is on what the screen
 * would then draw. That covers the seam between the two — which is where F-07 lived, the fatal
 * error the waiting room never collected, leaving people sitting in a room that no longer existed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WaitingRoomViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val wire = Json { classDiscriminator = "type"; encodeDefaults = true }

    private fun repo() = OnlineGameRepository(
        serverUrl = "ws://test.invalid",
        client = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
    )

    private suspend fun OnlineGameRepository.receive(message: ServerMessage) =
        handleServerMessage(wire.encodeToString(ServerMessage.serializer(), message))

    private fun room(
        players: List<RoomPlayerInfo>,
        host: String = "player_0",
        phase: RoomPhase = RoomPhase.WAITING,
        target: Int = 4,
    ) = RoomState(
        roomCode = "ABC123",
        phase = phase,
        players = players,
        hostPlayerId = host,
        targetPlayerCount = target,
    )

    private fun player(id: String, name: String, team: String, host: Boolean = false, connected: Boolean = true) =
        RoomPlayerInfo(id = id, name = name, teamId = team, isBot = false, isConnected = connected, isHost = host)

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun theRoomOnScreenFollowsTheRoomOnTheServer() = runTest(dispatcher) {
        val repo = repo()
        val vm = WaitingRoomViewModel(repo)
        runCurrent()

        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))
        repo.receive(
            ServerMessage.RoomUpdate(
                room(listOf(player("player_0", "Me", "team_1", host = true), player("player_1", "Vinu", "team_2")))
            )
        )
        runCurrent()

        val ui = vm.uiState.value
        assertEquals("ABC123", ui.roomCode)
        assertEquals(2, ui.players.size)
        assertEquals(4, ui.targetPlayerCount)
        assertEquals(listOf("Me", "Vinu"), ui.players.map { it.name })
    }

    @Test
    fun onlyTheHostIsToldTheyAreTheHost() = runTest(dispatcher) {
        val repo = repo()
        val vm = WaitingRoomViewModel(repo)
        runCurrent()

        // We are player_1; player_0 holds the room.
        repo.receive(ServerMessage.RoomCreated("ABC123", "player_1", 3, "tok"))
        repo.receive(
            ServerMessage.RoomUpdate(
                room(listOf(player("player_0", "Me", "team_1", host = true), player("player_1", "Vinu", "team_2")))
            )
        )
        runCurrent()

        // Only the host gets the Start button; showing it to everyone means taps that go nowhere.
        assertFalse(vm.uiState.value.isHost)
        assertEquals("player_1", vm.uiState.value.myPlayerId)
    }

    @Test
    fun aDisconnectedSeatIsShownAsAway() = runTest(dispatcher) {
        val repo = repo()
        val vm = WaitingRoomViewModel(repo)
        runCurrent()

        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))
        repo.receive(
            ServerMessage.RoomUpdate(
                room(listOf(player("player_0", "Me", "team_1", host = true), player("player_1", "Vinu", "team_2", connected = false)))
            )
        )
        runCurrent()

        // The seat is held for five minutes; the room has to say so or it looks like a ghost.
        val vinu = vm.uiState.value.players.single { it.name == "Vinu" }
        assertFalse(vinu.isConnected)
    }

    @Test
    fun aTerminalSessionErrorReachesTheScreen() = runTest(dispatcher) {
        val repo = repo()
        val vm = WaitingRoomViewModel(repo)
        runCurrent()

        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))
        repo.receive(ServerMessage.Error("Room not found"))
        runCurrent()

        // F-07: this was never collected, so people sat in a dead room under a green banner.
        assertEquals(FatalSessionError.ROOM_GONE, vm.fatalError.value)
    }

    @Test
    fun anOrdinaryErrorIsShownAndThenClearedByTheNextRoomUpdate() = runTest(dispatcher) {
        val repo = repo()
        val vm = WaitingRoomViewModel(repo)
        runCurrent()

        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))
        repo.receive(ServerMessage.Error("Room is full"))
        runCurrent()
        assertEquals("Room is full", vm.uiState.value.errorMessage)

        // A fresh room state means we are validly connected again — a stale message must not
        // linger underneath the reconnected banner.
        repo.receive(ServerMessage.RoomUpdate(room(listOf(player("player_0", "Me", "team_1", host = true)))))
        runCurrent()

        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun aPlayerComingAndGoingIsAnnouncedToTheScreen() = runTest(dispatcher) {
        val repo = repo()
        val vm = WaitingRoomViewModel(repo)
        val seen = mutableListOf<PlayerConnectionEvent>()
        backgroundScope.launch { vm.snackbarEvents.collect { seen += it } }
        runCurrent()

        repo.receive(ServerMessage.HostTransferred("player_1", "Vinu"))
        runCurrent()

        assertTrue(seen.any { it is PlayerConnectionEvent.HostChanged })
    }
}
