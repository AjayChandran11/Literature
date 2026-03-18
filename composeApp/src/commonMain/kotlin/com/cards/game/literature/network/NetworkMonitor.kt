package com.cards.game.literature.network

import kotlinx.coroutines.flow.StateFlow

expect object NetworkMonitor {
    val isNetworkAvailable: StateFlow<Boolean>
    fun startMonitoring()
    fun stopMonitoring()
}
