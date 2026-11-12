package com.cards.game.literature.server

import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.ClaimDeclaration
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.GameState
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.model.Player
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.Team
import com.cards.game.literature.protocol.PushMessage
import com.cards.game.literature.protocol.RoomPhase
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * When a seat is told something by push. The rule under test is the same everywhere: only a seat
 * whose socket is gone is pushed, only when it has a token, and never after its owner left on
 * purpose. Anyone connected is looking at the table.
 */
class PushNotificationsTest {

    private class RecordingPushSender : PushSender {
        data class Sent(val token: String, val type: PushType, val data: Map<String, String>, val onInvalidToken: () -> Unit)
        val sent = CopyOnWriteArrayList<Sent>()
        override fun send(token: String, type: PushType, data: Map<String, String>, onInvalidToken: () -> Unit) {
            sent += Sent(token, type, data, onInvalidToken)
        }
        fun of(type: PushType) = sent.filter { it.type == type }
        fun tokens(type: PushType) = of(type).map { it.token }
    }

    private fun room(code: String, sender: PushSender, seats: Int = 4) = GameRoom(code, seats, pushSender = sender)

    /** Seats [name] with a push token; the seat starts connected (the socket itself is not needed here). */
    private fun GameRoom.seat(name: String, token: String? = "tok-$name"): String {
        val id = addPlayer(name, isHost = getHumanPlayerCount() == 0)
        if (token != null) registerPushToken(id, token)
        return id
    }

    private fun GameRoom.away(id: String) { getPlayerSession(id)!!.isConnected = false }

    private fun card(suit: Suit, value: CardValue) = Card(suit, value)

    /**
     * Four players, two a side, on a hand-built deal: A on turn with two low spades, B with one
     * more and a heart, C with a spade and a heart, D with the rest. Enough for one failed ask
     * (A asks B for a spade C holds) and one successful one (A asks B for the spade B holds).
     */
    private fun midGame(a: String, b: String, c: String, d: String) = GameState(
        gameId = "g",
        players = listOf(
            Player(a, "A", "team_1", hand = listOf(card(Suit.SPADES, CardValue.TWO), card(Suit.SPADES, CardValue.THREE))),
            Player(b, "B", "team_2", hand = listOf(card(Suit.SPADES, CardValue.FOUR), card(Suit.HEARTS, CardValue.TWO))),
            Player(c, "C", "team_1", hand = listOf(card(Suit.SPADES, CardValue.FIVE), card(Suit.HEARTS, CardValue.THREE))),
            Player(d, "D", "team_2", hand = listOf(card(Suit.SPADES, CardValue.SIX), card(Suit.SPADES, CardValue.SEVEN), card(Suit.HEARTS, CardValue.FOUR))),
        ),
        teams = listOf(Team("team_1", "Team 1", listOf(a, c)), Team("team_2", "Team 2", listOf(b, d))),
        currentPlayerIndex = 0,
        phase = GamePhase.IN_PROGRESS,
        playerCount = 4,
    )

    /** One claim from the end: every half-suit but low spades is taken, 4–3 to A's team, and A holds all six. */
    private fun lastClaim(a: String, b: String, c: String, d: String): GameState {
        val lowSpades = listOf(CardValue.TWO, CardValue.THREE, CardValue.FOUR, CardValue.FIVE, CardValue.SIX, CardValue.SEVEN)
            .map { card(Suit.SPADES, it) }
        val claimed = HalfSuit.entries.mapIndexed { i, hs ->
            if (hs == HalfSuit.SPADES_LOW) HalfSuitStatus(hs)
            else HalfSuitStatus(hs, claimedByTeamId = if (i % 2 == 1) "team_1" else "team_2", claimCorrect = true)
        }
        return GameState(
            gameId = "g",
            players = listOf(
                Player(a, "A", "team_1", hand = lowSpades),
                Player(b, "B", "team_2"),
                Player(c, "C", "team_1"),
                Player(d, "D", "team_2"),
            ),
            teams = listOf(
                Team("team_1", "Team 1", listOf(a, c), score = claimed.count { it.claimedByTeamId == "team_1" }),
                Team("team_2", "Team 2", listOf(b, d), score = claimed.count { it.claimedByTeamId == "team_2" }),
            ),
            currentPlayerIndex = 0,
            phase = GamePhase.IN_PROGRESS,
            halfSuitStatuses = claimed,
            playerCount = 4,
        )
    }

    private fun GameState.claimLowSpades(claimer: String) = ClaimDeclaration(
        claimerId = claimer,
        halfSuit = HalfSuit.SPADES_LOW,
        cardAssignments = mapOf(claimer to getPlayer(claimer)!!.hand),
    )

    @Test
    fun theDealReachesWhoeverSteppedAway() = runBlocking {
        val sender = RecordingPushSender()
        val room = room("DEAL", sender)
        room.seat("A")
        val b = room.seat("B")
        room.away(b)

        assertTrue(room.startGame(fillWithBots = true))

        assertEquals(listOf("tok-B"), sender.tokens(PushType.GAME_STARTED), "only the absent seat is told; A is watching")
        val push = sender.of(PushType.GAME_STARTED).single()
        assertEquals(PushMessage.TYPE_GAME_STARTED, push.data[PushMessage.KEY_TYPE])
        assertEquals("DEAL", push.data[PushMessage.KEY_ROOM_CODE])
        assertTrue(sender.of(PushType.YOUR_TURN).isEmpty(), "nobody away is on turn: B's seat is a bot until they return")
        room.cleanup()
    }

    @Test
    fun yourTurnIsPushedOncePerTurnAndOnlyToSomeoneAway() = runBlocking {
        val sender = RecordingPushSender()
        val room = room("TURN", sender)
        val a = room.seat("A"); val b = room.seat("B"); val c = room.seat("C"); val d = room.seat("D")
        room.installRunningGameForTest(midGame(a, b, c, d))
        room.away(b)

        // A keeps the turn with a card B holds: the turn did not change hands, nothing to push.
        room.processAsk(a, b, listOf(card(Suit.SPADES, CardValue.FOUR)))
        assertTrue(sender.sent.isEmpty(), "a successful ask is the same turn")

        // Then asks for a spade C holds: the turn passes to B, who is not here to see it.
        room.processAsk(a, b, listOf(card(Suit.SPADES, CardValue.FIVE)))
        assertEquals(listOf("tok-B"), sender.tokens(PushType.YOUR_TURN))
        assertEquals("TURN", sender.of(PushType.YOUR_TURN).single().data[PushMessage.KEY_ROOM_CODE])
        room.cleanup()
    }

    @Test
    fun aTurnThatLandsOnSomeoneConnectedIsNotPushed() = runBlocking {
        val sender = RecordingPushSender()
        val room = room("HERE", sender)
        val a = room.seat("A"); val b = room.seat("B"); val c = room.seat("C"); val d = room.seat("D")
        room.installRunningGameForTest(midGame(a, b, c, d))

        room.processAsk(a, b, listOf(card(Suit.SPADES, CardValue.FIVE))) // fails, turn to B, who is connected
        assertTrue(sender.sent.isEmpty())
        room.cleanup()
    }

    @Test
    fun droppingOnYourOwnTurnIsToldAtOnce() = runBlocking {
        val sender = RecordingPushSender()
        val room = room("DROP", sender)
        val a = room.seat("A"); val b = room.seat("B"); val c = room.seat("C"); val d = room.seat("D")
        room.installRunningGameForTest(midGame(a, b, c, d))

        room.handleDisconnect(a) // the turn began while A's socket looked alive; no push had gone out
        assertEquals(listOf("tok-A"), sender.tokens(PushType.YOUR_TURN))

        room.handleDisconnect(c) // not on turn: nothing to say
        assertEquals(1, sender.sent.size)
        room.cleanup()
    }

    @Test
    fun anArrivalIsToldToWhoeverSteppedAwayAndNobodyElse() = runBlocking {
        val sender = RecordingPushSender()
        val room = room("WAIT", sender)
        val host = room.seat("Host")
        room.seat("Guest")
        room.seat("Quiet", token = null)
        room.away(host)
        room.away(room.getPlayerSession("player_2")!!.playerId)

        val seat = room.trySeat("Newcomer") as GameRoom.SeatResult.Seated
        room.notifyArrival(seat.playerId)

        val pushes = sender.of(PushType.PLAYER_JOINED)
        assertEquals(listOf("tok-Host"), pushes.map { it.token }, "the connected guest sees it; the tokenless seat cannot be reached")
        assertEquals("Newcomer", pushes.single().data[PushMessage.KEY_PLAYER_NAME])
        room.cleanup()
    }

    @Test
    fun theResultReachesWhoeverSteppedAwayFromEitherSide() = runBlocking {
        val sender = RecordingPushSender()
        val room = room("OVER", sender)
        val a = room.seat("A"); val b = room.seat("B"); val c = room.seat("C"); val d = room.seat("D")
        val state = lastClaim(a, b, c, d)
        room.installRunningGameForTest(state)
        room.away(b); room.away(c)

        room.processClaim(a, state.claimLowSpades(a))

        assertEquals(RoomPhase.FINISHED, room.phase)
        val byToken = sender.of(PushType.GAME_OVER).associate { it.token to it.data[PushMessage.KEY_RESULT] }
        assertEquals(mapOf("tok-B" to PushMessage.RESULT_LOST, "tok-C" to PushMessage.RESULT_WON), byToken)
        room.cleanup()
    }

    @Test
    fun someoneWhoLeftOnPurposeIsToldNothingMore() = runBlocking {
        val sender = RecordingPushSender()
        val room = room("LEFT", sender)
        val a = room.seat("A"); val b = room.seat("B"); val c = room.seat("C"); val d = room.seat("D")
        val state = lastClaim(a, b, c, d)
        room.installRunningGameForTest(state)
        room.handleIntentionalLeave(b)
        room.away(c)

        room.processClaim(a, state.claimLowSpades(a))

        assertEquals(listOf("tok-C"), sender.tokens(PushType.GAME_OVER), "B quit; only C, who merely stepped away, hears the result")
        room.cleanup()
    }

    @Test
    fun aRematchReachesTheSeatsKeptThroughIt() = runBlocking {
        val sender = RecordingPushSender()
        val room = room("AGAIN", sender)
        val a = room.seat("A"); val b = room.seat("B"); val c = room.seat("C"); val d = room.seat("D")
        val state = lastClaim(a, b, c, d)
        room.installRunningGameForTest(state)
        room.processClaim(a, state.claimLowSpades(a))
        sender.sent.clear()

        room.handleDisconnect(b) // on the result screen; the seat is held for the reconnect window
        assertTrue(room.resetForRematch())

        assertEquals(listOf("tok-B"), sender.tokens(PushType.REMATCH))
        assertEquals(RoomPhase.WAITING, room.phase)
        room.cleanup()
    }

    @Test
    fun aTokenTheServiceCallsDeadIsForgotten() = runBlocking {
        val sender = RecordingPushSender()
        val room = room("DEAD", sender)
        room.seat("A")
        val b = room.seat("B")
        room.away(b)
        assertTrue(room.startGame(fillWithBots = true))

        sender.of(PushType.GAME_STARTED).single().onInvalidToken()
        assertNull(room.getPlayerSession(b)!!.pushToken, "no more pushes to a token FCM has retired")

        room.registerPushToken(b, "tok-B2")
        sender.of(PushType.GAME_STARTED).single().onInvalidToken()
        assertEquals("tok-B2", room.getPlayerSession(b)!!.pushToken, "a late verdict on an old token leaves the new one alone")
        room.cleanup()
    }

    @Test
    fun onlySomethingShapedLikeATokenIsKept() {
        val room = room("JUNK", PushSender.None)
        val a = room.seat("A", token = null)
        val session = room.getPlayerSession(a)!!

        listOf("", "   ", "has spaces inside", "<script>alert(1)</script>", "x".repeat(5000)).forEach {
            room.registerPushToken(a, it)
            assertNull(session.pushToken, "rejected: ${it.take(20)}")
        }
        room.registerPushToken(a, "  cXyZ123:APA91b-Hello_World.ok  ")
        assertEquals("cXyZ123:APA91b-Hello_World.ok", session.pushToken)
        room.cleanup()
    }
}
