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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cards.game.literature.bot.BotPersonalities
import com.cards.game.literature.viewmodel.PassSelectionUiState
import com.cards.game.literature.viewmodel.PlayerInfo
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Option C: shown while a correct claim has suspended the game for the claimer
 * to choose who plays next. The claimer gets a picker; everyone else gets a
 * non-dismissable "waiting on the claimer" indicator. Both vanish automatically
 * once the suspension resolves (the caller stops passing a [PassSelectionUiState]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassTargetSheet(
    passSelection: PassSelectionUiState,
    onSelect: (String) -> Unit
) {
    if (passSelection.isMine) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        // Dismissing without choosing must not wedge the game: fall back to the
        // first eligible teammate — the same default the server timeout uses.
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
                Text(
                    text = stringResource(Res.string.pass_select_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.pass_select_subtitle),
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
    } else {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(Res.string.pass_waiting, passSelection.claimerName),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PassCandidateRow(candidate: PlayerInfo, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
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
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(Res.string.pass_cards_left, candidate.cardCount),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
