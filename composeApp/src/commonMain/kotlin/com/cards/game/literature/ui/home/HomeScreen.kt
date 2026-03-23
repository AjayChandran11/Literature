package com.cards.game.literature.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cards.game.literature.preferences.SessionStore
import com.cards.game.literature.ui.theme.CardRed
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartGame: (playerName: String, playerCount: Int) -> Unit,
    onPlayOnline: (playerName: String) -> Unit = {}
) {
    val session = koinInject<SessionStore>()
    var playerName by remember { mutableStateOf(session.playerName) }
    var showSetupDialog by remember { mutableStateOf(false) }
    val onBackground = MaterialTheme.colorScheme.onBackground

    Box(modifier = Modifier.fillMaxSize()) {
        // Corner suit symbols
        val cornerSuits = listOf(
            stringResource(Res.string.suit_spades),
            stringResource(Res.string.suit_hearts),
            stringResource(Res.string.suit_diamonds),
            stringResource(Res.string.suit_clubs)
        )
        val cornerColors = listOf(onBackground, CardRed, CardRed, onBackground)
        val cornerAlignments = listOf(
            Alignment.TopStart, Alignment.TopEnd,
            Alignment.BottomEnd, Alignment.BottomStart
        )
        cornerSuits.forEachIndexed { i, suit ->
            Text(
                suit,
                fontSize = 52.sp,
                color = cornerColors[i].copy(alpha = 0.12f),
                modifier = Modifier
                    .align(cornerAlignments[i])
                    .padding(40.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.suits_display),
                fontSize = 48.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.home_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.home_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it; session.playerName = it },
                label = { Text(stringResource(Res.string.home_player_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showSetupDialog = true },
                enabled = playerName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.outline
                )
            ) {
                Text(stringResource(Res.string.home_new_game), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { onPlayOnline(playerName.trim()) },
                enabled = playerName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text(stringResource(Res.string.home_play_online), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showSetupDialog) {
        GameSetupDialog(
            onDismiss = { showSetupDialog = false },
            onConfirm = { playerCount ->
                showSetupDialog = false
                onStartGame(playerName.trim(), playerCount)
            }
        )
    }
}

@Composable
fun GameSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    confirmLabel: String = stringResource(Res.string.button_start_game),
    allowEightPlayers: Boolean = false
) {
    var selectedCount by remember { mutableIntStateOf(4) }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = stringResource(Res.string.suits_display),
                    fontSize = 22.sp,
                    color = secondary,
                    letterSpacing = 6.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.game_setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Text(
                    text = stringResource(Res.string.game_setup_players_question),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Player count cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    (if (allowEightPlayers) listOf(4, 6, 8) else listOf(4, 6)).forEach { count ->
                        val isSelected = selectedCount == count
                        val teams = count / 2

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) primary.copy(alpha = 0.12f)
                                    else surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) primary else primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedCount = count }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$count",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) primary else onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.game_setup_players_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(Res.string.game_setup_teams_format, teams, teams),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action buttons
                Button(
                    onClick = { onConfirm(selectedCount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text(
                        confirmLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(Res.string.button_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
