package com.cards.game.literature.ui.lobby

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

    LaunchedEffect(Unit) {
        viewModel.navigateToGame.collect {
            onGameStart()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Waiting Room",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
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
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.roomCode,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    color = GoldAccent
                )
                Text(
                    text = "Share this code with friends",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Players (${uiState.players.size}/${uiState.targetPlayerCount})",
            fontSize = 18.sp,
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = if (player.teamId == "team_1") "Team 1" else "Team 2",
                            fontSize = 14.sp,
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
                                    fontSize = 10.sp,
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
                    fontSize = 14.sp,
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
                    Text("Start Game", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Text(
                text = "Waiting for host to start...",
                fontSize = 16.sp,
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
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            LaunchedEffect(error) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearError()
            }
        }
    }
}
