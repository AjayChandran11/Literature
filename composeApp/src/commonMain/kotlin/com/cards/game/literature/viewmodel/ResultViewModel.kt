package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ResultUiState(
    val myTeamScore: Int = 0,
    val opponentTeamScore: Int = 0,
    val myTeamName: String = "Your Team",
    val opponentTeamName: String = "Opponents",
    val isWinner: Boolean = false,
    val isDraw: Boolean = false,
    val halfSuitBreakdown: List<HalfSuitStatus> = emptyList()
)

class ResultViewModel(
    private val repository: GameRepository,
    private val myPlayerId: String = "player_0"
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    init {
        val state = repository.gameState.value
        if (state != null && state.phase == GamePhase.FINISHED) {
            val myTeam = state.getTeamForPlayer(myPlayerId)
            val opponentTeam = state.teams.firstOrNull { it.id != myTeam?.id }
            val myScore = myTeam?.score ?: 0
            val oppScore = opponentTeam?.score ?: 0
            _uiState.value = ResultUiState(
                myTeamScore = myScore,
                opponentTeamScore = oppScore,
                myTeamName = myTeam?.name ?: "Your Team",
                opponentTeamName = opponentTeam?.name ?: "Opponents",
                isWinner = myScore > oppScore,
                isDraw = myScore == oppScore,
                halfSuitBreakdown = state.halfSuitStatuses
            )
        }
    }
}
