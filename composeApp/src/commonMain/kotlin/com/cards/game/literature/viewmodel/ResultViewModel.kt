package com.cards.game.literature.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.preferences.TutorialPrefs
import com.cards.game.literature.protocol.RoomPhase
import com.cards.game.literature.repository.GameRepository
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.review.AppReview
import com.cards.game.literature.stats.Achievement
import com.cards.game.literature.stats.StatsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultUiState(
    val myTeamScore: Int = 0,
    val opponentTeamScore: Int = 0,
    val myTeamName: String = "",
    val opponentTeamName: String = "",
    /** The local player's team id — used to attribute half-suit claims to "my team" vs the
     *  opponents. Must not be assumed "team_1": online players can be on team 2. */
    val myTeamId: String = "",
    val isWinner: Boolean = false,
    val isDraw: Boolean = false,
    val halfSuitBreakdown: List<HalfSuitStatus> = emptyList(),
    val gameLog: List<GameEvent> = emptyList(),
    val unlockedAchievements: List<Achievement> = emptyList(),
    /** True when this is an online game and the local player is the host. */
    val canRematch: Boolean = false,
    /** True only on the player's first completed game — gates the one-time debrief. */
    val isFirstGame: Boolean = false
)

// Don't prompt for a review until the player has a few games behind them — a first-time winner
// hasn't formed an opinion yet, and Play's quota is a scarce resource not to spend on them.
private const val REVIEW_MIN_GAMES = 3

class ResultViewModel(
    private val repository: GameRepository,
    private val myPlayerId: String = "player_0"
) : ViewModel() {

    private val onlineRepository = repository as? OnlineGameRepository

    // Identifies THIS game's pending celebration so a stale one can't leak onto a later
    // game's result screen (captured at construction — the result screen shows a FINISHED game).
    private val myGameId: String? = repository.gameState.value?.gameId

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    /** Room code for rematch navigation (online only). */
    val roomCode: String get() = onlineRepository?.roomCode ?: ""

    // Set when the host's rematch resets the room, BEFORE the result screen navigates to
    // the waiting room. onCleared() reads it to keep the connection alive on a rematch
    // (vs. closing it on a real exit to Home).
    private var isRematching = false

    // replay=1: the init block may emit BEFORE the screen's collector subscribes
    // (fast-host rematch detected at construction). Safe here — this ViewModel
    // lives for exactly one result screen, so the replayed value can't leak into
    // a later game the way a repository-level replay would.
    private val _rematchStarted = MutableSharedFlow<Unit>(replay = 1)
    /** Emits when the host resets the room — everyone navigates back. */
    val rematchStarted: Flow<Unit> = _rematchStarted.asSharedFlow()

    fun requestRematch() {
        viewModelScope.launch {
            onlineRepository?.requestRematch()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Leaving the result screen to Home ends the online session. The game→result
        // transition deliberately keeps the socket open (so Rematch works), so we close it
        // here on a genuine exit — leaveRoomAndReset() also tells the server and clears the
        // repo identity so a network blip can't auto-reconnect us to the abandoned room. On a
        // rematch we navigate to the waiting room instead and must keep the connection.
        if (!isRematching) onlineRepository?.leaveRoomAndReset()
    }

    init {
        // Intercept the repo's rematch signal so isRematching is set BEFORE the result
        // screen navigates away (onCleared relies on it to keep the connection alive).
        viewModelScope.launch {
            onlineRepository?.rematchStarted?.collect {
                isRematching = true
                _rematchStarted.emit(Unit)
            }
        }

        // A fast host can reset the room while this player is still watching the
        // match finale — the repo's one-shot rematch signal then fired before this
        // ViewModel existed and was dropped, and the game state is already cleared.
        // The aftermath is durable though: no game state + room back in WAITING.
        // Detect it and re-raise the signal so this player follows into the waiting
        // room instead of stranding on an empty result screen (where Home would
        // even leave the room the others are sitting in).
        if (onlineRepository != null &&
            repository.gameState.value == null &&
            onlineRepository.roomState.value?.phase == RoomPhase.WAITING
        ) {
            isRematching = true
            _rematchStarted.tryEmit(Unit)
        }

        val state = repository.gameState.value
        if (state != null && state.phase == GamePhase.FINISHED) {
            val myTeam = state.getTeamForPlayer(myPlayerId)
            val opponentTeam = state.teams.firstOrNull { it.id != myTeam?.id }
            val myScore = myTeam?.score ?: 0
            val oppScore = opponentTeam?.score ?: 0
            // First-game debrief: gamesPlayed<=1 excludes veterans (updating users have many
            // games), the flag makes it strictly once. gamesPlayed may still be 0 here if
            // recording is in flight, but 0 and 1 both satisfy <=1, so it's read-order safe.
            val isFirstGame = !TutorialPrefs.isFirstGameDebriefShown() &&
                StatsStore.stats.value.gamesPlayed <= 1
            _uiState.value = ResultUiState(
                myTeamScore = myScore,
                opponentTeamScore = oppScore,
                myTeamName = myTeam?.name ?: "",
                opponentTeamName = opponentTeam?.name ?: "",
                myTeamId = myTeam?.id ?: "",
                isWinner = myScore > oppScore,
                isDraw = myScore == oppScore,
                halfSuitBreakdown = state.halfSuitStatuses,
                gameLog = state.events,
                canRematch = onlineRepository != null &&
                    onlineRepository.roomState.value?.hostPlayerId == myPlayerId,
                isFirstGame = isFirstGame
            )
            if (isFirstGame) TutorialPrefs.markFirstGameDebriefShown()

            // A win is a natural high point to ask for a rating. Skip first-timers; Play's own
            // quota decides whether the sheet actually shows and rate-limits how often.
            if (myScore > oppScore && StatsStore.stats.value.gamesPlayed >= REVIEW_MIN_GAMES) {
                AppReview.requestReview()
            }
        }

        // Collect (rather than read once) — game recording may still be in flight when this
        // ViewModel is created. Accept only the celebration for THIS game (gameId match), so a
        // stale unlock can't leak onto a later game and a screen recreation re-reads it.
        viewModelScope.launch {
            StatsStore.pendingCelebration.collect { pending ->
                if (pending != null && pending.gameId == myGameId && pending.achievements.isNotEmpty()) {
                    _uiState.update { it.copy(unlockedAchievements = pending.achievements) }
                }
            }
        }
    }
}
