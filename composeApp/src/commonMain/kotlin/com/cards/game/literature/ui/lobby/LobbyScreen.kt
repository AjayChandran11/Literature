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
import androidx.compose.ui.unit.sp
import com.cards.game.literature.network.NetworkMonitor
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.LobbyViewModel
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
            text = "Play Online",
            style = MaterialTheme.typography.headlineLarge,
            color = GoldAccent
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Playing as $playerName",
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
            Text("Create Room", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Join Room
        Text(
            text = "or join with a code",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = joinRoomCode,
            onValueChange = { joinRoomCode = it.uppercase().take(6) },
            label = { Text("Room Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldAccent,
                focusedLabelColor = GoldAccent,
                cursorColor = GoldAccent
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
                Text("Join Room", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = onBack) {
            Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { playerCount ->
                showCreateDialog = false
                viewModel.createRoom(playerName, playerCount)
            }
        )
    }
}

@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedCount by remember { mutableIntStateOf(6) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Room", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select number of players:", color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(16.dp))
                listOf(4, 6, 8).forEach { count ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCount == count,
                            onClick = { selectedCount = count },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "$count Players (${count / 2}v${count / 2})",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedCount) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
