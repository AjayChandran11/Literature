package com.cards.game.literature.ui.navigation

import androidx.activity.compose.BackHandler
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
import com.cards.game.literature.ui.common.ConnectionBanner
import com.cards.game.literature.ui.game.GameBoardContent
import com.cards.game.literature.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
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
    var isQuitting by remember { mutableStateOf(false) }
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
            title = { Text(stringResource(Res.string.dialog_leave_online_game_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.dialog_leave_online_game_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        if (isQuitting) return@Button
                        isQuitting = true
                        showQuitDialog = false
                        scope.launch { onlineRepository.leaveGame() }
                        onQuit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.button_leave_game))
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text(stringResource(Res.string.button_keep_playing))
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
            ConnectionBanner(connectionState = onlineRepository.connectionState)

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
                stringResource(Res.string.reconnect_waiting, info.playerName, secondsLeft),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
