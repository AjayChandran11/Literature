package com.cards.game.literature.bot

import com.cards.game.literature.model.GameState
import kotlinx.coroutines.delay

class BotPlayer(
    private val strategy: BotStrategy = BotStrategy()
) {
    suspend fun decideMove(state: GameState, botId: String): BotAction {
        // Simulate thinking time
        delay((1500L..2500L).random())
        return strategy.decideMove(state, botId)
    }
}
