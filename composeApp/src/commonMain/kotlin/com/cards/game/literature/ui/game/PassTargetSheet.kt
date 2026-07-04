package com.cards.game.literature.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.bot.BotPersonalities
import com.cards.game.literature.model.currentTimeMillis
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.viewmodel.PassSelectionUiState
import com.cards.game.literature.viewmodel.PlayerInfo
import kotlinx.coroutines.delay
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Option C: the claimer's picker, shown after a correct claim empties their hand
 * while 2+ teammates are still active. ONLY shown to the claimer (isMine) — other
 * players see a non-blocking "choosing…" state in the turn banner instead, since
 * the pause is enforced by game state and there is nothing for them to do.
 * Dismissing without a pick falls back to the default (first eligible teammate),
 * matching the server timeout, so the game can never wedge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassTargetSheet(
    passSelection: PassSelectionUiState,
    onSelect: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultTarget = passSelection.candidates.firstOrNull()?.id
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { defaultTarget?.let(onSelect) },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title + a top-right countdown to the server's auto-pick deadline, so
            // the claimer knows they have limited time to choose (online only —
            // deadlineMs is null offline, where there's no timeout).
            val seconds = rememberPassCountdownSeconds(passSelection.deadlineMs)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.pass_select_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                seconds?.let { s ->
                    Spacer(Modifier.width(12.dp))
                    PassCountdownChip(s)
                }
            }
            Text(
                // Echo the claim result — the claimer's sheet covers the event
                // strip, so this confirms what they just claimed before they pick.
                text = passSelection.claimedHalfSuitLabel?.let {
                    stringResource(Res.string.pass_select_subtitle_claimed, it)
                } ?: stringResource(Res.string.pass_select_subtitle),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            passSelection.candidates.forEach { candidate ->
                PassCandidateRow(candidate = candidate, onClick = { onSelect(candidate.id) })
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PassCandidateRow(candidate: PlayerInfo, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        // surfaceVariant is a muted tone in BOTH schemes; secondaryContainer maps
        // to a bright gold in dark mode that glares against the navy sheet.
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (candidate.isBot) BotPersonalities.emojiFor(candidate.name)
                    else candidate.name.take(1).uppercase(),
                    fontSize = 18.sp
                )
            }
            Text(
                text = candidate.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(Res.string.pass_cards_left, candidate.cardCount),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/** Calm countdown pill for the claimer's sheet header — no pulse (they're actively
 *  deciding), just turns red + bold under 10s to convey urgency. */
@Composable
private fun PassCountdownChip(seconds: Int) {
    val urgent = seconds <= 10
    val color = if (urgent) CardRed else MaterialTheme.colorScheme.secondary
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(
            text = stringResource(Res.string.game_timer_seconds, seconds),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Seconds remaining until [deadlineMs] (server's Option C auto-pick), ticking once
 * per second; null when there's no deadline (offline — no timeout). Shared by the
 * claimer's sheet chip and the other-players' turn-banner indicator so both count
 * off the same source. Keyed on deadlineMs, so it resets cleanly per pending episode.
 */
@Composable
internal fun rememberPassCountdownSeconds(deadlineMs: Long?): Int? {
    var remaining by remember(deadlineMs) {
        mutableStateOf(deadlineMs?.let { ((it - currentTimeMillis()) / 1000L).coerceIn(0L, 99L).toInt() })
    }
    LaunchedEffect(deadlineMs) {
        if (deadlineMs == null) return@LaunchedEffect
        while (true) {
            val r = ((deadlineMs - currentTimeMillis()) / 1000L).coerceAtLeast(0L).toInt()
            remaining = r
            if (r <= 0) break
            delay(1000L)
        }
    }
    return remaining
}
