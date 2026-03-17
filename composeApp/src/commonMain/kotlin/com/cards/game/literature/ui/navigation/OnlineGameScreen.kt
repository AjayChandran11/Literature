package com.cards.game.literature.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.repository.ConnectionState
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.ui.game.GameBoardContent
import com.cards.game.literature.viewmodel.GameViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named

@Composable
fun OnlineGameScreen(
    onlineRepository: OnlineGameRepository,
    onGameEnd: () -> Unit,
    onQuit: () -> Unit,
    viewModel: GameViewModel = koinViewModel(qualifier = named("online"))
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by onlineRepository.connectionState.collectAsState()
    var showQuitDialog by remember { mutableStateOf(false) }

    BackHandler { showQuitDialog = true }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("Leave Game?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave? A bot will take over your turn.") },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        onlineRepository.disconnect()
                        onQuit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text("Keep Playing")
                }
            }
        )
    }

    // Navigate to result when game ends
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == GamePhase.FINISHED) {
            onGameEnd()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameBoardContent(viewModel = viewModel)

        // Connection status overlay
        AnimatedVisibility(
            visible = connectionState == ConnectionState.RECONNECTING || connectionState == ConnectionState.DISCONNECTED,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (connectionState == ConnectionState.RECONNECTING)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                        else
                            MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (connectionState == ConnectionState.RECONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Reconnecting...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        Text(
                            "Disconnected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}
