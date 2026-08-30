package com.cards.game.literature.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private fun isDocumentHidden(): Boolean = js("document.hidden")

private fun onVisibilityChange(callback: () -> Unit): Unit =
    js("document.addEventListener('visibilitychange', callback)")

actual object AppLifecycleObserver {
    private val _isAppInForeground = MutableStateFlow(true)
    actual val isAppInForeground: StateFlow<Boolean> = _isAppInForeground

    private var attached = false

    fun init() {
        if (attached) return
        attached = true
        _isAppInForeground.value = !isDocumentHidden()
        onVisibilityChange { _isAppInForeground.value = !isDocumentHidden() }
    }
}
