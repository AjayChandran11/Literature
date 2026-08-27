package com.cards.game.literature.bot

import com.cards.game.literature.model.GameState
import kotlinx.coroutines.delay

class BotPlayer(
    private val strategy: BotStrategy = BotStrategy(),
    private val difficulty: BotDifficulty = BotDifficulty.MEDIUM
) {
    suspend fun decideMove(
        state: GameState,
        botId: String,
        // Simulated thinking time — consistent across difficulties. Overridable so the
        // client can expose a bot-speed setting; the server keeps the default.
        thinkingDelayMillis: LongRange = 3_500L..4_000L
    ): BotAction {
        delay(thinkingDelayMillis.random())
        return strategy.decideMove(state, botId, difficulty)
    }
}
