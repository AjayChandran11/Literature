package com.cards.game.literature.ui.lobby

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cards.game.literature.network.NetworkMonitor
import com.cards.game.literature.notifications.RequestNotificationPermissionOnce
import com.cards.game.literature.ui.home.GameSetupDialog
import com.cards.game.literature.viewmodel.LoadingOperation
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
    val snackbarHostState = remember { SnackbarHostState() }

    RequestNotificationPermissionOnce()

    LaunchedEffect(Unit) {
        viewModel.navigateToWaitingRoom.collect { roomCode ->
            onNavigateToWaitingRoom(roomCode)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
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
                if (uiState.loadingOperation == LoadingOperation.CREATE) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        stringResource(Res.string.lobby_create_room),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                if (uiState.loadingOperation == LoadingOperation.JOIN) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        stringResource(Res.string.lobby_join_room),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Text(stringResource(Res.string.button_back), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AnimatedVisibility(
                visible = uiState.showWarmingUp,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Text(
                    text = stringResource(Res.string.lobby_server_warming_up),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
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
