package com.cards.game.literature.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private fun isNavigatorOnline(): Boolean = js("navigator.onLine")

private fun onOnlineChange(callback: (Boolean) -> Unit): Unit = js(
    """(function() {
        window.addEventListener('online', function() { callback(true); });
        window.addEventListener('offline', function() { callback(false); });
    })()"""
)

actual object NetworkMonitor {
    private val _isNetworkAvailable = MutableStateFlow(true)
    actual val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var isMonitoring = false

    actual fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        _isNetworkAvailable.value = isNavigatorOnline()
        onOnlineChange { online -> _isNetworkAvailable.value = online }
    }

    actual fun stopMonitoring() {
        // Listeners live for the tab's lifetime; nothing to tear down.
        isMonitoring = false
    }
}
