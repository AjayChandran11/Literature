package com.cards.game.literature.ui.common

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.cards.game.literature.isWebPlatform
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Desktop browsers scroll with a wheel/trackpad, which a horizontal LazyRow ignores on web;
 * bridge Scroll pointer events into the row. Returns this unchanged off web, so Android/iOS
 * touch behavior is untouched.
 */
@Composable
fun Modifier.webWheelScroll(state: LazyListState): Modifier {
    if (!isWebPlatform()) return this
    val scope = rememberCoroutineScope()
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
                        scope.launch { state.scrollBy(px) }
                    }
                }
            }
        }
    }
}
