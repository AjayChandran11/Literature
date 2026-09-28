package com.cards.game.literature.repository

import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.Team
import com.cards.game.literature.protocol.PlayerGameView
import com.cards.game.literature.protocol.PublicPlayerInfo
import com.cards.game.literature.protocol.RoomPlayerInfo
import com.cards.game.literature.protocol.RoomPhase
import com.cards.game.literature.protocol.RoomState
import com.cards.game.literature.protocol.ServerMessage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The server-message pipeline: raw frame in, player-visible state out.
 *
 * This is the riskiest file in the client — 792 lines carrying four of the September audit's P1s
 * — and it had no tests. Everything here is a decision that strands a real player when it goes
 * wrong: a misread error leaves them on a frozen board under a green "Reconnected" banner, or
 * bounces them home from a room that was fine.
 *
 * Frames are built from the real protocol types and serialized with the same configuration the
 * repository decodes with, so a wire-shape change breaks these rather than sliding through.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OnlineGameRepositoryMessageTest {

    private val wire = Json { classDiscriminator = "type"; encodeDefaults = true }

    /** An engine that answers nothing: no test here should touch the network. */
    private fun repo(): OnlineGameRepository {
        val client = HttpClient(MockEngine { respondError(HttpStatusCode.ServiceUnavailable) })
        return OnlineGameRepository(serverUrl = "ws://test.invalid", client = client)
    }

    private suspend fun OnlineGameRepository.receive(message: ServerMessage) =
        handleServerMessage(wire.encodeToString(ServerMessage.serializer(), message))

    private fun view(
        gameId: String = "g1",
        phase: GamePhase = GamePhase.IN_PROGRESS,
        deadline: Long? = null,
    ) = PlayerGameView(
        gameId = gameId,
        myPlayerId = "player_0",
        myHand = listOf(Card(Suit.SPADES, CardValue.TWO)),
        players = listOf(
            PublicPlayerInfo("player_0", "Me", "team_1", 1, false),
            PublicPlayerInfo("player_1", "Vinu", "team_2", 1, false),
        ),
        teams = listOf(
            Team("team_1", "Team 1", listOf("player_0")),
            Team("team_2", "Team 2", listOf("player_1")),
        ),
        currentPlayerId = "player_0",
        phase = phase,
        halfSuitStatuses = emptyList(),
        recentEvents = emptyList(),
        turnDeadlineMs = deadline,
    )

    private fun room(phase: RoomPhase = RoomPhase.WAITING) = RoomState(
        roomCode = "ABC123",
        phase = phase,
        players = listOf(RoomPlayerInfo("player_0", "Me", "team_1", false, true, true)),
        hostPlayerId = "player_0",
        targetPlayerCount = 4,
    )

    // ─── identity ────────────────────────────────────────────────────────────

    @Test
    fun roomCreatedAdoptsTheSeatTheServerHandedUs() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))

        assertEquals("ABC123", repo.roomCode)
        assertEquals("player_0", repo.myPlayerId)
    }

    @Test
    fun roomUpdatePublishesTheRoom() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.RoomUpdate(room()))

        assertEquals("ABC123", repo.roomState.value?.roomCode)
        assertEquals(RoomPhase.WAITING, repo.roomState.value?.phase)
    }

    // ─── the board ───────────────────────────────────────────────────────────

    @Test
    fun gameUpdatePublishesTheBoard() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.GameUpdate(view()))

        val state = repo.gameState.value
        assertNotNull(state)
        assertEquals("g1", state.gameId)
        assertEquals(GamePhase.IN_PROGRESS, state.phase)
        assertEquals(2, state.players.size)
    }

    @Test
    fun theTurnDeadlineRidesThroughToTheBoard() = runTest {
        val repo = repo()
        // The countdown is only correct because this value survives the whole pipeline. Dropped
        // anywhere along it, observers go back to watching a stuck red "0s".
        repo.receive(ServerMessage.GameUpdate(view(deadline = 1_700_000_000_000)))

        assertEquals(1_700_000_000_000, repo.gameState.value?.turnDeadlineMs)
    }

    @Test
    fun aViewWithNoClockRunningCarriesNoDeadline() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.GameUpdate(view(deadline = null)))

        assertNull(repo.gameState.value?.turnDeadlineMs)
    }

    @Test
    fun eventsAccumulateIntoTheMatchLog() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.GameUpdate(view()))
        repo.receive(ServerMessage.GameEventOccurred(GameEvent.TurnChanged("player_1", "Vinu", 1L)))
        repo.receive(ServerMessage.GameEventOccurred(GameEvent.TurnChanged("player_0", "Me", 2L)))

        // The wire view carries only a short tail; the result screen's full log is this list.
        assertEquals(2, repo.eventLog.size)
    }

    // ─── errors that end a session, and errors that don't ────────────────────

    @Test
    fun roomGoneWhileSeatedIsFatal() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))

        repo.receive(ServerMessage.Error("Room not found"))

        assertEquals(FatalSessionError.ROOM_GONE, repo.fatalError.value)
    }

    @Test
    fun aWrongInviteCodeIsAnOrdinaryLobbyErrorNotAFatalOne() = runTest {
        val repo = repo()
        repo.roomCode = "ZZZZZZ" // joinRoom fills this BEFORE admission
        // No seat yet: this is someone mistyping a code, not a room disappearing under them.
        // Treated as fatal it showed the wrong message and left the bad code in place.
        repo.receive(ServerMessage.Error("Room not found"))

        assertNull(repo.fatalError.value, "a bad code must not end the session")
        assertEquals("", repo.roomCode, "and the bad code is not kept")
    }

    @Test
    fun aLostSeatIsRetriedOnceBeforeGivingUp() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))
        repo.myPlayerName = "Vinu"

        // Away past the waiting-room window. The room is still there and the server hands back a
        // same-name seat, so the client re-joins itself rather than stranding the player.
        repo.receive(ServerMessage.Error("Player not found in room"))

        assertNull(repo.fatalError.value, "the first loss is retried, not reported")
    }

    @Test
    fun aSecondLostSeatIsReportedAsSeatLost() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))
        repo.myPlayerName = "Vinu"

        repo.receive(ServerMessage.Error("Player not found in room"))
        repo.receive(ServerMessage.Error("Player not found in room"))

        assertEquals(FatalSessionError.SEAT_LOST, repo.fatalError.value)
    }

    @Test
    fun anInvalidSessionTokenEndsTheSessionWhenThereIsNoNameToRejoinWith() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))
        // No remembered name: nothing to re-join as.
        repo.receive(ServerMessage.Error("Session invalid"))

        assertEquals(FatalSessionError.SEAT_LOST, repo.fatalError.value)
    }

    @Test
    fun anUnrelatedServerErrorIsSurfacedWithoutEndingTheSession() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))

        val seen = mutableListOf<String>()
        backgroundScope.launch { repo.errors.collect { seen += it } }
        runCurrent() // let the collector subscribe before anything is emitted

        repo.receive(ServerMessage.Error("Not your turn"))
        runCurrent()

        // Shown to the player, but the session survives: an ordinary rejection is not a reason
        // to tear the game down.
        assertEquals(listOf("Not your turn"), seen)
        assertNull(repo.fatalError.value)
    }

    @Test
    fun roomClosedEndsTheSession() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.RoomCreated("ABC123", "player_0", 3, "tok"))

        repo.receive(ServerMessage.RoomClosed)

        assertEquals(ConnectionState.DISCONNECTED, repo.connectionState.value)
    }

    // ─── who else is at the table ────────────────────────────────────────────

    @Test
    fun aNewHostIsAnnounced() = runTest {
        val repo = repo()
        val events = mutableListOf<PlayerConnectionEvent>()
        backgroundScope.launch { repo.playerEvents.collect { events += it } }
        runCurrent()

        repo.receive(ServerMessage.HostTransferred("player_1", "Vinu"))
        runCurrent()

        // The host badge and whether Rematch is offered both follow this.
        val changed = events.filterIsInstance<PlayerConnectionEvent.HostChanged>()
        assertEquals(1, changed.size, "exactly one host change announced")
        assertEquals("Vinu", changed.single().newHostName)
    }

    // ─── robustness ──────────────────────────────────────────────────────────

    @Test
    fun aFrameThisClientCannotUnderstandIsIgnored() = runTest {
        val repo = repo()
        repo.receive(ServerMessage.GameUpdate(view()))
        val before = repo.gameState.value

        // A newer server sending a message type this build has never heard of must not take the
        // game down; the player carries on with the board they have.
        repo.handleServerMessage("""{"type":"com.example.NotAThing","whatever":1}""")
        repo.handleServerMessage("not json at all")

        assertEquals(before?.gameId, repo.gameState.value?.gameId)
        assertNull(repo.fatalError.value)
    }
}
