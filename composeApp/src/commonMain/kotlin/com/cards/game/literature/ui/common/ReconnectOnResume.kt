package com.cards.game.literature.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cards.game.literature.repository.ConnectionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Kicks a reconnect when the screen comes back to the foreground while the online session is
 * down. The socket rarely survives a long background, and after the reconnect budget is spent
 * nothing else retries — returning to the app is the natural moment to try again.
 */
@Composable
fun ReconnectOnResume(connectionState: StateFlow<ConnectionState>, trigger: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val state = connectionState.value
                if (state == ConnectionState.DISCONNECTED || state == ConnectionState.RECONNECTING) trigger()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
