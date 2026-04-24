package com.cards.game.literature.notifications

import kotlinx.coroutines.flow.StateFlow

expect object AppLifecycleObserver {
    val isAppInForeground: StateFlow<Boolean>
}
