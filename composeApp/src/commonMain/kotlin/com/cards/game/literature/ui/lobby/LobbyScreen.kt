package com.cards.game.literature.ui.lobby

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cards.game.literature.network.NetworkMonitor
import com.cards.game.literature.ui.home.GameSetupDialog
import com.cards.game.literature.viewmodel.LobbyViewModel
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    playerName: String,
    onNavigateToWaitingRoom: (roomCode: String) -> Unit,
    onBack: () -> Unit,
    viewModel: LobbyViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isNetworkAvailable by NetworkMonitor.isNetworkAvailable.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var joinRoomCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.navigateToWaitingRoom.collect { roomCode ->
            onNavigateToWaitingRoom(roomCode)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.home_play_online),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.lobby_playing_as, playerName),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        // Create Room
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !uiState.isLoading && isNetworkAvailable
        ) {
            Text(stringResource(Res.string.lobby_create_room), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Join Room
        Text(
            text = stringResource(Res.string.lobby_join_with_code),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = joinRoomCode,
            onValueChange = { joinRoomCode = it.uppercase().take(6) },
            label = { Text(stringResource(Res.string.lobby_room_code_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                cursorColor = MaterialTheme.colorScheme.secondary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.joinRoom(joinRoomCode, playerName) },
            enabled = joinRoomCode.length == 6 && !uiState.isLoading && isNetworkAvailable,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(Res.string.lobby_join_room), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = onBack) {
            Text(stringResource(Res.string.button_back), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
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

    if (showCreateDialog) {
        GameSetupDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { playerCount, _ ->
                showCreateDialog = false
                viewModel.createRoom(playerName, playerCount)
            },
            confirmLabel = stringResource(Res.string.lobby_create_room),
            allowEightPlayers = true,
            showDifficulty = false
        )
    }
}
