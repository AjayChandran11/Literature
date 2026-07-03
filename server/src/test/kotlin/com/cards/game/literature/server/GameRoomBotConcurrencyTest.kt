package com.cards.game.literature.server

import com.cards.game.literature.bot.BotAction
import com.cards.game.literature.logic.DeckUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression guard for the production crash where two executeBotTurns() loops ran
 * concurrently and the loser applied a move that had become illegal, throwing an
 * uncaught IllegalArgumentException that killed the coroutine and wedged the game.
 *
 * The trigger was a disconnect→replaceWithBot() (hence checkNextTurn()) firing
 * while a bot was still "thinking" (deciding its move outside the mutex). This test
 * parks a bot mid-decision, fires a second turn-progression trigger, and asserts
 * that a second loop never starts. On the pre-fix code the second checkNextTurn()
 * launched an overlapping loop and this assertion fails.
 */
class GameRoomBotConcurrencyTest {

    @Test
    fun secondTurnTriggerDoesNotStartAnOverlappingBotLoop() = runBlocking {
        val room = GameRoom("TEST01", 4)
        val alice = room.addPlayer("Alice", isHost = true) // player_0 (human)
        val bob = room.addPlayer("Bob")                    // player_1 (human)
        val humans = listOf(alice, bob)                    // player_2/3 will be bots

        val inFlight = AtomicInteger(0)   // bot decisions currently executing
        val maxInFlight = AtomicInteger(0)
        val firstEntered = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>() // never completed — cleanup() cancels the loop

        // A bot whose decision parks indefinitely, so the loop that owns it stays
        // "in flight" while we fire the second trigger. It records how many loops
        // are ever inside a decision at once.
        room.decideBotMove = { _, _ ->
            val depth = inFlight.incrementAndGet()
            maxInFlight.updateAndGet { m -> maxOf(m, depth) }
            firstEntered.complete(Unit)
            try {
                gate.await()
            } finally {
                inFlight.decrementAndGet()
            }
            BotAction.Ask("player_0", DeckUtils.createFullDeck().first()) // unreachable
        }

        withTimeout(10_000) {
            room.startGame(fillWithBots = true)

            // Ensure exactly one bot loop is running and parked in the decision.
            // If a human opens, converting them to a bot starts the loop; if a bot
            // opens, startGame() already started it.
            val opener = room.currentPlayerId
            val convertedOpener = if (opener in humans) {
                room.handleIntentionalLeave(opener!!)
                opener
            } else null
            firstEntered.await()

            // Fire a SECOND turn-progression trigger (another disconnect→
            // replaceWithBot) while the first loop still holds the turn.
            val secondLeaver = humans.first { it != convertedOpener }
            room.handleIntentionalLeave(secondLeaver)

            // Give any (buggy) second loop time to reach the decision point.
            delay(500)
            assertEquals(
                1, maxInFlight.get(),
                "a second executeBotTurns loop must never run while one is already active"
            )
        }

        room.cleanup()
    }
}
