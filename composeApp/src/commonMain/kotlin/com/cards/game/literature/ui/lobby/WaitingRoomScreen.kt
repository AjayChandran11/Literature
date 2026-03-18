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
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.WaitingRoomViewModel
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

    BackHandler {
        showLeaveDialog = true
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Room?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave the room?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveDialog = false
                        viewModel.leaveRoom()
                        onLeave()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Stay")
                }
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigateToGame.collect {
            onGameStart()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { scaffoldPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Waiting Room",
            style = MaterialTheme.typography.headlineMedium,
            color = GoldAccent
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
                    text = "Room Code",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.roomCode,
                    style = MaterialTheme.typography.displaySmall,
                    letterSpacing = 4.sp,
                    color = GoldAccent
                )
                Text(
                    text = "Share this code with friends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Players (${uiState.players.size}/${uiState.targetPlayerCount})",
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
                            text = if (player.teamId == "team_1") "Team 1" else "Team 2",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (player.isHost) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = GoldAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "HOST",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Checkbox(
                    checked = fillWithBots,
                    onCheckedChange = { fillWithBots = it },
                    colors = CheckboxDefaults.colors(checkedColor = GoldAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Fill empty slots with bots",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.startGame(fillWithBots) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !uiState.isStarting && (fillWithBots || uiState.players.size == uiState.targetPlayerCount)
            ) {
                if (uiState.isStarting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Start Game", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Text(
                text = "Waiting for host to start...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = {
            viewModel.leaveRoom()
            onLeave()
        }) {
            Text("Leave Room", color = MaterialTheme.colorScheme.error)
        }

        uiState.errorMessage?.let { error ->
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
    } // Scaffold
}
