package com.cards.game.literature.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_get_status
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
actual object NetworkMonitor {
    private val _isNetworkAvailable = MutableStateFlow(true)
    actual val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val monitor = nw_path_monitor_create()
    private var isMonitoring = false

    actual fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        nw_path_monitor_set_update_handler(monitor) { path ->
            _isNetworkAvailable.value = nw_path_get_status(path) == nw_path_status_satisfied
        }
        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)
    }

    actual fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        nw_path_monitor_cancel(monitor)
    }
}
