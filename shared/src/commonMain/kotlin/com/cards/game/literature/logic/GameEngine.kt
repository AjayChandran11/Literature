package com.cards.game.literature.logic

import com.cards.game.literature.bot.BotPersonalities
import com.cards.game.literature.model.*

data class GameResult(
    val newState: GameState,
    val events: List<GameEvent>
)

data class PlayerSetupInfo(
    val id: String,
    val name: String,
    val teamId: String,
    val isBot: Boolean = false
)

class GameEngine {

    fun createGame(
        gameId: String,
        playerName: String,
        playerCount: Int
    ): GameState {
        require(playerCount in listOf(4, 6, 8)) { "Player count must be 4, 6, or 8" }

        val humanPlayer = Player(id = "player_0", name = playerName, teamId = "team_1")
        val botPlayers = (1 until playerCount).map { i ->
            val teamId = if (i % 2 == 0) "team_1" else "team_2"
            Player(
                id = "player_$i",
                name = getBotName(i),
                teamId = teamId,
                isBot = true
            )
        }

        val allPlayers = listOf(humanPlayer) + botPlayers

        val team1Ids = allPlayers.filter { it.teamId == "team_1" }.map { it.id }
        val team2Ids = allPlayers.filter { it.teamId == "team_2" }.map { it.id }

        val teams = listOf(
            Team(id = "team_1", name = "Your Team", playerIds = team1Ids),
            Team(id = "team_2", name = "Opponents", playerIds = team2Ids)
        )

        // Deal cards with a recorded seed so the game is reproducible
        val dealSeed = kotlin.random.Random.nextLong()
        val hands = CardDealer.dealCards(playerCount, dealSeed)
        val playersWithCards = allPlayers.mapIndexed { index, player ->
            player.copy(hand = hands[index].sortedWith(compareBy({ it.suit }, { it.value.rank })))
        }

        return GameState(
            gameId = gameId,
            players = playersWithCards,
            teams = teams,
            currentPlayerIndex = 0,
            phase = GamePhase.IN_PROGRESS,
            playerCount = playerCount,
            events = listOf(GameEvent.GameStarted(playerCount)),
            dealSeed = dealSeed
        )
    }

    fun createMultiplayerGame(
        gameId: String,
        players: List<PlayerSetupInfo>
    ): GameState {
        val playerCount = players.size
        require(playerCount in listOf(4, 6, 8)) { "Player count must be 4, 6, or 8" }

        val allPlayers = players.map { setup ->
            Player(
                id = setup.id,
                name = setup.name,
                teamId = setup.teamId,
                isBot = setup.isBot
            )
        }

        val team1Ids = allPlayers.filter { it.teamId == "team_1" }.map { it.id }
        val team2Ids = allPlayers.filter { it.teamId == "team_2" }.map { it.id }

        val teams = listOf(
            Team(id = "team_1", name = "Team 1", playerIds = team1Ids),
            Team(id = "team_2", name = "Team 2", playerIds = team2Ids)
        )

        // Deal cards with a recorded seed so the game is reproducible
        val dealSeed = kotlin.random.Random.nextLong()
        val hands = CardDealer.dealCards(playerCount, dealSeed)
        val playersWithCards = allPlayers.mapIndexed { index, player ->
            player.copy(hand = hands[index].sortedWith(compareBy({ it.suit }, { it.value.rank })))
        }

        return GameState(
            gameId = gameId,
            players = playersWithCards,
            teams = teams,
            currentPlayerIndex = 0,
            phase = GamePhase.IN_PROGRESS,
            playerCount = playerCount,
            events = listOf(GameEvent.GameStarted(playerCount)),
            dealSeed = dealSeed
        )
    }

    fun processAsk(
        state: GameState,
        askerId: String,
        targetId: String,
        card: Card,
        batchId: String? = null
    ): GameResult {
        val validation = MoveValidator.validateAsk(state, askerId, targetId, card)
        require(validation.isValid) { validation.errorMessage ?: "Invalid ask" }

        val target = state.getPlayer(targetId)!!
        val asker = state.getPlayer(askerId)!!
        val hasCard = card in target.hand

        val newEvents = mutableListOf<GameEvent>()

        val event = GameEvent.CardAsked(
            askerId = askerId,
            askerName = asker.name,
            targetId = targetId,
            targetName = target.name,
            card = card,
            success = hasCard,
            batchId = batchId
        )
        newEvents.add(event)

        val newPlayers = if (hasCard) {
            // Transfer card from target to asker
            state.players.map { player ->
                when (player.id) {
                    targetId -> player.copy(hand = player.hand - card)
                    askerId -> player.copy(
                        hand = (player.hand + card).sortedWith(compareBy({ it.suit }, { it.value.rank }))
                    )
                    else -> player
                }
            }
        } else {
            state.players
        }

        // Determine next player
        val nextPlayerIndex = if (hasCard) {
            // Asker gets another turn (if they still have cards)
            val askerIdx = state.players.indexOfFirst { it.id == askerId }
            if (newPlayers[askerIdx].isActive) askerIdx
            else findNextActivePlayer(newPlayers, askerIdx)
        } else {
            // Turn passes to the target (if they have cards)
            val targetIdx = state.players.indexOfFirst { it.id == targetId }
            if (newPlayers[targetIdx].isActive) targetIdx
            else findNextActivePlayer(newPlayers, targetIdx)
        }

        val turnEvent = GameEvent.TurnChanged(
            newPlayerId = newPlayers[nextPlayerIndex].id,
            newPlayerName = newPlayers[nextPlayerIndex].name
        )
        newEvents.add(turnEvent)

        var newState = state.copy(
            players = newPlayers,
            currentPlayerIndex = nextPlayerIndex,
            events = state.events + newEvents
        )

        // Check for early game end
        newState = checkForEarlyGameEnd(newState)

        return GameResult(newState, newEvents)
    }

    fun processClaim(
        state: GameState,
        declaration: ClaimDeclaration,
        // When true, a correct claim that empties the claimer's hand while 2+
        // teammates are still active suspends the game (sets pendingPass) instead
        // of auto-passing, so the claimer can choose who plays next (Option C).
        // Callers that can't/shouldn't offer the choice — bots, legacy clients —
        // leave this false and get the original deterministic first-teammate pass.
        pauseForPassSelection: Boolean = false
    ): GameResult {
        val validation = MoveValidator.validateClaim(state, declaration)
        require(validation.isValid) { validation.errorMessage ?: "Invalid claim" }

        val result = ClaimEvaluator.evaluate(state, declaration)
        val claimer = state.getPlayer(declaration.claimerId)!!

        val newEvents = mutableListOf<GameEvent>()

        val event = GameEvent.DeckClaimed(
            claimerId = declaration.claimerId,
            claimerName = claimer.name,
            teamId = result.awardedToTeamId,
            halfSuit = declaration.halfSuit,
            correct = result.correct
        )
        newEvents.add(event)

        // Update half-suit status
        val newStatuses = state.halfSuitStatuses.map { status ->
            if (status.halfSuit == declaration.halfSuit) {
                status.copy(claimedByTeamId = result.awardedToTeamId, claimCorrect = result.correct)
            } else status
        }

        // Update team scores
        val newTeams = state.teams.map { team ->
            if (team.id == result.awardedToTeamId) team.copy(score = team.score + 1)
            else team
        }

        // Remove claimed cards from all players' hands
        val claimedCards = DeckUtils.getAllCardsForHalfSuit(declaration.halfSuit).toSet()
        val newPlayers = state.players.map { player ->
            player.copy(hand = player.hand.filter { it !in claimedCards })
        }

        // Determine next player
        val claimerIdx = state.players.indexOfFirst { it.id == declaration.claimerId }
        val claimerNowActive = newPlayers[claimerIdx].isActive

        // Teammates who could take the turn when a correct claim empties the
        // claimer. Empty in every other case. A correct claim only removes the
        // claiming team's own cards, so it can never empty the opposing team or
        // finish the game while these teammates are still active — meaning the
        // pause branch below is always mid-game with a real turn still to give.
        val eligibleTeammates: List<Player> = if (result.correct && !claimerNowActive) {
            val claimerTeam = state.teams.first { it.playerIds.contains(declaration.claimerId) }
            newPlayers.filter {
                it.id in claimerTeam.playerIds && it.id != declaration.claimerId && it.isActive
            }
        } else emptyList()

        // Option C: suspend instead of auto-passing when there's a real choice.
        if (pauseForPassSelection && eligibleTeammates.size >= 2) {
            val pausedState = state.copy(
                players = newPlayers,
                teams = newTeams,
                currentPlayerIndex = claimerIdx, // stays on the now-empty claimer until they pick
                halfSuitStatuses = newStatuses,
                events = state.events + newEvents, // DeckClaimed only; TurnChanged comes on resolution
                pendingPass = PendingPass(declaration.claimerId, eligibleTeammates.map { it.id })
            )
            return GameResult(pausedState, newEvents)
        }

        val nextPlayerIndex = if (result.correct) {
            // Claimer keeps turn if they still have cards
            if (claimerNowActive) claimerIdx
            else {
                // Claimer is out of cards — pass to the first active teammate (deterministic)
                if (eligibleTeammates.isNotEmpty()) {
                    newPlayers.indexOf(eligibleTeammates.first())
                } else {
                    findNextActivePlayer(newPlayers, claimerIdx)
                }
            }
        } else {
            // Turn goes to next opponent
            val opposingTeam = state.teams.first { it.id != result.claimingTeamId }
            val nextOpponent = newPlayers.indexOfFirst { it.id in opposingTeam.playerIds && it.isActive }
            if (nextOpponent >= 0) nextOpponent
            else findNextActivePlayer(newPlayers, claimerIdx)
        }

        val turnEvent = GameEvent.TurnChanged(
            newPlayerId = newPlayers[nextPlayerIndex].id,
            newPlayerName = newPlayers[nextPlayerIndex].name
        )
        newEvents.add(turnEvent)

        // Check if game is over (all 8 half-suits claimed)
        val allClaimed = newStatuses.all { it.claimedByTeamId != null }
        val phase = if (allClaimed) GamePhase.FINISHED else state.phase

        var newState = state.copy(
            players = newPlayers,
            teams = newTeams,
            currentPlayerIndex = nextPlayerIndex,
            halfSuitStatuses = newStatuses,
            phase = phase,
            events = state.events + newEvents
        )

        if (phase == GamePhase.FINISHED) {
            val winnerTeamId = newTeams.maxByOrNull { it.score }?.let { winner ->
                if (newTeams.count { it.score == winner.score } == 1) winner.id else null
            }
            newEvents.add(GameEvent.GameEnded(winnerTeamId))
            newState = newState.copy(events = newState.events + GameEvent.GameEnded(winnerTeamId))
        }

        // Check for early game end
        newState = checkForEarlyGameEnd(newState)

        return GameResult(newState, newEvents)
    }

    /**
     * Resolves an Option C suspension: [selectedPlayerId] (chosen by the claimer,
     * or the fallback default on timeout/disconnect/bot/legacy) takes the next
     * turn. Validates the pick against [GameState.pendingPass], moves the turn,
     * clears the suspension, and emits the deferred TurnChanged. Idempotent by
     * contract — callers must only invoke it while a pass is pending.
     */
    fun applyPassSelection(state: GameState, selectedPlayerId: String): GameResult {
        val pending = state.pendingPass
        require(pending != null) { "No pending pass selection to resolve" }
        require(selectedPlayerId in pending.eligibleTeammateIds) {
            "Player $selectedPlayerId is not an eligible pass target"
        }
        val targetIdx = state.players.indexOfFirst { it.id == selectedPlayerId }
        require(targetIdx >= 0 && state.players[targetIdx].isActive) {
            "Player $selectedPlayerId is no longer active"
        }

        val turnEvent = GameEvent.TurnChanged(
            newPlayerId = state.players[targetIdx].id,
            newPlayerName = state.players[targetIdx].name
        )
        val newState = state.copy(
            currentPlayerIndex = targetIdx,
            pendingPass = null,
            events = state.events + turnEvent
        )
        return GameResult(newState, listOf(turnEvent))
    }

    fun findNextActivePlayer(players: List<Player>, fromIndex: Int): Int {
        val count = players.size
        for (i in 1..count) {
            val idx = (fromIndex + i) % count
            if (players[idx].isActive) return idx
        }
        // No active players — should not happen normally
        return fromIndex
    }

    private fun checkForEarlyGameEnd(state: GameState): GameState {
        if (state.phase == GamePhase.FINISHED) return state

        // Check if all players on one team have 0 cards
        for (team in state.teams) {
            val teamPlayers = state.players.filter { it.id in team.playerIds }
            if (teamPlayers.all { !it.isActive }) {
                // This team is out of cards — unclaimed half-suits go to the other team
                val otherTeam = state.teams.first { it.id != team.id }
                var newStatuses = state.halfSuitStatuses
                var bonusPoints = 0
                for (status in state.halfSuitStatuses) {
                    if (status.claimedByTeamId == null) {
                        newStatuses = newStatuses.map {
                            if (it.halfSuit == status.halfSuit) it.copy(claimedByTeamId = otherTeam.id, claimCorrect = null)
                            else it
                        }
                        bonusPoints++
                    }
                }
                if (bonusPoints > 0) {
                    val newTeams = state.teams.map {
                        if (it.id == otherTeam.id) it.copy(score = it.score + bonusPoints)
                        else it
                    }
                    val winnerTeamId = newTeams.maxByOrNull { it.score }?.let { winner ->
                        if (newTeams.count { it.score == winner.score } == 1) winner.id else null
                    }
                    return state.copy(
                        halfSuitStatuses = newStatuses,
                        teams = newTeams,
                        phase = GamePhase.FINISHED,
                        events = state.events + GameEvent.GameEnded(winnerTeamId)
                    )
                }
            }
        }

        // Also check if all half-suits are claimed
        if (state.halfSuitStatuses.all { it.claimedByTeamId != null }) {
            val winnerTeamId = state.teams.maxByOrNull { it.score }?.let { winner ->
                if (state.teams.count { it.score == winner.score } == 1) winner.id else null
            }
            return state.copy(
                phase = GamePhase.FINISHED,
                events = state.events + GameEvent.GameEnded(winnerTeamId)
            )
        }

        return state
    }

    private fun getBotName(index: Int): String =
        BotPersonalities.ALL.map { it.name }.getOrElse(index - 1) { "Bot $index" }
}
