package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.repository.GameRepository
import com.cards.game.literature.stats.Achievement
import com.cards.game.literature.stats.StatsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultUiState(
    val myTeamScore: Int = 0,
    val opponentTeamScore: Int = 0,
    val myTeamName: String = "",
    val opponentTeamName: String = "",
    val isWinner: Boolean = false,
    val isDraw: Boolean = false,
    val halfSuitBreakdown: List<HalfSuitStatus> = emptyList(),
    val gameLog: List<GameEvent> = emptyList(),
    val unlockedAchievements: List<Achievement> = emptyList()
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
                myTeamName = myTeam?.name ?: "",
                opponentTeamName = opponentTeam?.name ?: "",
                isWinner = myScore > oppScore,
                isDraw = myScore == oppScore,
                halfSuitBreakdown = state.halfSuitStatuses,
                gameLog = state.events
            )
        }

        // Collect (rather than read once) — game recording may still be
        // in flight when this ViewModel is created. Once displayed, the
        // unlocks stick to this screen's state and the store is cleared.
        viewModelScope.launch {
            StatsStore.pendingCelebration.collect { unlocked ->
                if (unlocked.isNotEmpty()) {
                    _uiState.update { it.copy(unlockedAchievements = unlocked) }
                    StatsStore.clearPendingCelebration()
                }
            }
        }
    }
}
