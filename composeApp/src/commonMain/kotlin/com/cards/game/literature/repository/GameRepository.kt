package com.cards.game.literature.repository

import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GameRepository {
    val gameState: StateFlow<GameState?>
    val gameEvents: Flow<GameEvent>

    suspend fun createGame(playerName: String, playerCount: Int, difficulty: BotDifficulty = BotDifficulty.MEDIUM): GameState
    suspend fun submitAsk(askerId: String, targetId: String, card: Card)
    suspend fun submitMultiAsk(askerId: String, targetId: String, cards: List<Card>)
    suspend fun submitClaim(declaration: ClaimDeclaration)

    /** Option C: the claimer's choice of which active teammate takes the turn
     *  after a correct claim emptied their hand. Only meaningful while
     *  [gameState]'s pendingPass is set; a no-op otherwise. */
    suspend fun submitPassTarget(selectedPlayerId: String)
}
