package com.cards.game.literature.notifications

import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.currentTimeMillis
import com.cards.game.literature.preferences.GamePrefs
import com.cards.game.literature.repository.OnlineGameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Subscribes to OnlineGameRepository + app lifecycle and fires local notifications
 * when the app is backgrounded. Foreground suppression is absolute — if the app is
 * visible we never notify, we only dismiss stale notifications.
 */
class NotificationCoordinator(
    private val onlineRepo: OnlineGameRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            AppLifecycleObserver.isAppInForeground.collect { inForeground ->
                if (inForeground) Notifier.clearAll()
            }
        }

        scope.launch {
            onlineRepo.gameEvents.collect { event ->
                if (!shouldNotify(event.timestamp)) return@collect
                when (event) {
                    is GameEvent.GameStarted -> Notifier.notifyGameStarting()
                    is GameEvent.GameEnded -> {
                        val myTeamId = onlineRepo.gameState.value?.players
                            ?.firstOrNull { it.id == onlineRepo.myPlayerId }?.teamId
                        val won = event.winnerTeamId != null && event.winnerTeamId == myTeamId
                        Notifier.notifyGameOver(won)
                    }
                    else -> {}
                }
            }
        }

        scope.launch {
            onlineRepo.gameState
                .map { state ->
                    val me = onlineRepo.myPlayerId
                    state != null && me.isNotEmpty() &&
                        state.players.getOrNull(state.currentPlayerIndex)?.id == me
                }
                .distinctUntilChanged()
                .collect { isMyTurn ->
                    if (!GamePrefs.isNotificationsEnabled()) return@collect
                    if (isMyTurn && !AppLifecycleObserver.isAppInForeground.value) {
                        Notifier.notifyYourTurn()
                    } else {
                        Notifier.clearYourTurn()
                    }
                }
        }
    }

    private fun shouldNotify(eventTimestampMs: Long): Boolean {
        if (!GamePrefs.isNotificationsEnabled()) return false
        if (AppLifecycleObserver.isAppInForeground.value) return false
        // Skip events older than 10s — avoids firing on replay after reconnect
        val age = currentTimeMillis() - eventTimestampMs
        return age in 0..10_000
    }
}
