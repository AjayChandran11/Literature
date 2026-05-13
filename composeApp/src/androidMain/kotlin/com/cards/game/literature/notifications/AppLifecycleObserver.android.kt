package com.cards.game.literature.notifications

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual object AppLifecycleObserver {
    private val _isAppInForeground = MutableStateFlow(false)
    actual val isAppInForeground: StateFlow<Boolean> = _isAppInForeground

    private var attached = false

    fun init() {
        if (attached) return
        attached = true
        Handler(Looper.getMainLooper()).post {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        _isAppInForeground.value = true
                    }
                    override fun onStop(owner: LifecycleOwner) {
                        _isAppInForeground.value = false
                    }
                }
            )
        }
    }
}
