package com.cards.game.literature.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cards.game.literature.model.currentTimeMillis
import kotlinx.coroutines.delay
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.game_timer_seconds
import org.jetbrains.compose.resources.stringResource

/**
 * The online turn countdown, isolated into its own leaf so ticking it recomposes ONLY this text
 * and not whatever row it sits in. Counts down to the server's [deadlineMs], which arrives with
 * every view: the server restarts its clock after EVERY move, including a successful ask that
 * keeps the turn, so a countdown the client drove itself drifted and could sit at a red "0s"
 * while the player still had most of a minute.
 *
 * Renders nothing when there is no clock (offline, a bot's turn, a pending pass) or above 15s.
 * Lives here rather than in the board file because the ask and claim sheets show it too: a modal
 * sheet covers the header, and the turn used to run out behind it with no warning at all.
 */
@Composable
internal fun TurnTimerText(
    deadlineMs: Long?,
    timerPaused: Boolean,
    style: TextStyle,
    normalColor: Color,
    spacer: Dp,
) {
    var secondsRemaining by remember { mutableStateOf(0) }
    LaunchedEffect(deadlineMs, timerPaused) {
        if (deadlineMs == null) { secondsRemaining = 0; return@LaunchedEffect }
        while (!timerPaused) {
            val left = ((deadlineMs - currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
            secondsRemaining = left.toInt()
            if (left <= 0L) break
            delay(250L) // re-read the clock rather than assume a tick took exactly a second
        }
    }
    if (deadlineMs != null && secondsRemaining in 1..15) {
        Spacer(modifier = Modifier.width(spacer))
        val label = stringResource(Res.string.game_timer_seconds, secondsRemaining)
        if (secondsRemaining <= 10) {
            // A pill, not just a red word. Colour alone carried the urgency, which says nothing
            // to a red-green colour-blind player, and plain red on the dark header sat near
            // 2.5:1. The error container pair is contrast-checked by the theme.
            //
            // Painted on the text's own line box, with horizontal padding only: any vertical
            // padding here makes this the tallest thing in its row and the whole row grows by
            // it the moment the clock passes ten seconds. The line box already carries the
            // font's ascent and descent, so the fill has its own breathing room.
            Text(
                label,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp),
                style = style,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            Text(label, style = style, fontWeight = FontWeight.Bold, color = normalColor)
        }
    }
}
