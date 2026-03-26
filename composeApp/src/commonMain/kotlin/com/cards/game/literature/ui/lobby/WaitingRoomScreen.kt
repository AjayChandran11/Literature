package com.cards.game.literature.ui.lobby

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.cards.game.literature.repository.PlayerConnectionEvent
import com.cards.game.literature.ui.common.ConnectionBanner
import com.cards.game.literature.viewmodel.WaitingRoomViewModel
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WaitingRoomScreen(
    onGameStart: () -> Unit,
    onLeave: () -> Unit,
    viewModel: WaitingRoomViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var fillWithBots by remember { mutableStateOf(true) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var isLeaving by remember { mutableStateOf(false) }

    BackHandler {
        showLeaveDialog = true
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(stringResource(Res.string.dialog_leave_room_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.dialog_leave_room_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        if (isLeaving) return@Button
                        isLeaving = true
                        showLeaveDialog = false
                        viewModel.leaveRoom()
                        onLeave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.button_leave))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text(stringResource(Res.string.button_stay))
                }
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    val disconnectedFmt = stringResource(Res.string.snackbar_player_disconnected)
    val reconnectedFmt = stringResource(Res.string.snackbar_player_reconnected)
    val hostChangedFmt = stringResource(Res.string.snackbar_host_changed)
    val replacedByBotFmt = stringResource(Res.string.snackbar_replaced_by_bot)
    val startGameTimeoutMsg = stringResource(Res.string.error_start_game_timeout)

    LaunchedEffect(Unit) {
        viewModel.navigateToGame.collect {
            onGameStart()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { event ->
            val message = when (event) {
                is PlayerConnectionEvent.Disconnected -> disconnectedFmt.format(event.playerName)
                is PlayerConnectionEvent.Reconnected -> reconnectedFmt.format(event.playerName)
                is PlayerConnectionEvent.HostChanged -> hostChangedFmt.format(event.newHostName)
                is PlayerConnectionEvent.ReplacedByBot -> replacedByBotFmt.format(event.playerName)
            }
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { scaffoldPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
    ) {
        ConnectionBanner(
            connectionState = viewModel.connectionState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(Res.string.waiting_room_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Room code display
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.waiting_room_code_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.roomCode,
                    style = MaterialTheme.typography.displaySmall,
                    letterSpacing = 4.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(Res.string.waiting_room_share_code),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.waiting_room_players_count, uiState.players.size, uiState.targetPlayerCount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Player list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.players) { player ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Connection indicator
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (player.isConnected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = if (player.teamId == "team_1")
                                stringResource(Res.string.waiting_room_team_1)
                            else
                                stringResource(Res.string.waiting_room_team_2),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (player.id == uiState.myPlayerId) {
                            Spacer(modifier = Modifier.width(4.dp))
                            TextButton(
                                onClick = { viewModel.switchTeam() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = stringResource(Res.string.waiting_room_switch_team),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (player.isHost) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = stringResource(Res.string.player_badge_host),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Host controls
        if (uiState.isHost) {
            val team1Count = uiState.players.count { it.teamId == "team_1" }
            val team2Count = uiState.players.count { it.teamId == "team_2" }
            val teamsUneven = !fillWithBots && team1Count != team2Count

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Checkbox(
                    checked = fillWithBots,
                    onCheckedChange = { fillWithBots = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.waiting_room_fill_bots),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (teamsUneven) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.waiting_room_teams_uneven_warning),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.startGame(fillWithBots) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isStarting && !teamsUneven && (fillWithBots || uiState.players.size == uiState.targetPlayerCount)
            ) {
                if (uiState.isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(Res.string.button_start_game), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Text(
                text = stringResource(Res.string.waiting_room_waiting_for_host),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                if (isLeaving) return@TextButton
                isLeaving = true
                viewModel.leaveRoom()
                onLeave()
            }
        ) {
            Text(stringResource(Res.string.button_leave_room), color = MaterialTheme.colorScheme.error)
        }

        val errorToShow = when {
            uiState.isStartGameTimedOut -> startGameTimeoutMsg
            else -> uiState.errorMessage
        }
        errorToShow?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearError()
            }
        }
    }
    } // Box
    } // Scaffold
}
