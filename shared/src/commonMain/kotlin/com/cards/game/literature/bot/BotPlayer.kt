package com.cards.game.literature.bot

import com.cards.game.literature.model.GameState
import kotlinx.coroutines.delay

class BotPlayer(
    private val strategy: BotStrategy = BotStrategy(),
    private val difficulty: BotDifficulty = BotDifficulty.MEDIUM
) {
    suspend fun decideMove(state: GameState, botId: String): BotAction {
        // Simulate thinking time — consistent across all difficulties
        delay((3500L..4000L).random())
        return strategy.decideMove(state, botId, difficulty)
    }
}
