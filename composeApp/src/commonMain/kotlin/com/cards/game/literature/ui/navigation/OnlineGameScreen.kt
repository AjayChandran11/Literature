package com.cards.game.literature.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.model.currentTimeMillis
import com.cards.game.literature.repository.ConnectionState
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.repository.ReconnectInfo
import com.cards.game.literature.ui.game.GameBoardContent
import com.cards.game.literature.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    val reconnectCountdowns by onlineRepository.reconnectCountdowns.collectAsState()
    var showQuitDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Reconnect on app resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.RECONNECTING) {
                    onlineRepository.triggerReconnect()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler { showQuitDialog = true }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("Leave Game?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave? A bot will take over immediately.") },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        scope.launch { onlineRepository.leaveGame() }
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

    GameBoardContent(
        viewModel = viewModel,
        headerOverlay = {
            // Connection status banner (slides down below score bar)
            AnimatedVisibility(
                visible = connectionState == ConnectionState.RECONNECTING || connectionState == ConnectionState.DISCONNECTED,
                enter = expandVertically(),
                exit = shrinkVertically()
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

            // Reconnect countdown banners for disconnected players
            ReconnectCountdownBanners(reconnectCountdowns)
        }
    )
}

@Composable
private fun ReconnectCountdownBanners(countdowns: Map<String, ReconnectInfo>) {
    if (countdowns.isEmpty()) return

    countdowns.forEach { (_, info) ->
        var secondsLeft by remember(info.deadlineMs) {
            val remaining = ((info.deadlineMs - currentTimeMillis()) / 1000).coerceAtLeast(0)
            mutableStateOf(remaining)
        }

        LaunchedEffect(info.deadlineMs) {
            while (secondsLeft > 0) {
                delay(1000L)
                val remaining = ((info.deadlineMs - currentTimeMillis()) / 1000).coerceAtLeast(0)
                secondsLeft = remaining
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Waiting for ${info.playerName} to reconnect... (${secondsLeft}s)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
