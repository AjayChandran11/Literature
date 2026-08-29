package com.cards.game.literature.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.cards.game.literature.isWebPlatform
import kotlin.math.abs

/**
 * Desktop browsers scroll with a wheel/trackpad, which a horizontal LazyRow ignores on web;
 * bridge Scroll pointer events into the row. Returns this unchanged off web, so Android/iOS
 * touch behavior is untouched.
 */
@Composable
fun Modifier.webWheelScroll(state: LazyListState): Modifier {
    if (!isWebPlatform()) return this
    return this.pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val delta = event.changes.fold(0f) { acc, change ->
                        acc + change.scrollDelta.x + change.scrollDelta.y
                    }
                    if (delta != 0f) {
                        event.changes.forEach { it.consume() }
                        // Wheels report notch counts (small values), trackpads report pixels.
                        val px = if (abs(delta) < 10f) delta * 40f else delta
                        // Synchronous raw dispatch: a coroutine-per-event via scrollBy made
                        // each mutation cancel the in-flight one, dropping deltas mid-fling.
                        state.dispatchRawDelta(px)
                    }
                }
            }
        }
    }
}
