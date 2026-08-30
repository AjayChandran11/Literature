package com.cards.game.literature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Desktop browsers give the phone-first UI absurd widths; cap it to a centred phone-ish
 * column over a felt backdrop. On phone-sized windows the cap exceeds the viewport width,
 * so this is a no-op there. Wide-screen-native layouts can replace this per screen later.
 */
@Composable
internal fun WebPageColumn(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F261F))) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .widthIn(max = 480.dp)
        ) {
            content()
        }
    }
}
