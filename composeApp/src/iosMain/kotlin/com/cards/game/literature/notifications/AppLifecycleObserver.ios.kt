package com.cards.game.literature.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationState

@OptIn(ExperimentalForeignApi::class)
actual object AppLifecycleObserver {
    private val _isAppInForeground = MutableStateFlow(
        UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive
    )
    actual val isAppInForeground: StateFlow<Boolean> = _isAppInForeground

    private var attached = false

    fun init() {
        if (attached) return
        attached = true
        val center = NSNotificationCenter.defaultCenter
        center.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            _isAppInForeground.value = true
        }
        center.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            _isAppInForeground.value = false
        }
    }
}
