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
import com.cards.game.literature.model.ReactionType
import com.cards.game.literature.ui.common.ConnectionBanner
import com.cards.game.literature.ui.game.DisplayReaction
import com.cards.game.literature.ui.game.FloatingReactions
import com.cards.game.literature.ui.game.GameBoardContent
import com.cards.game.literature.ui.game.ReactionPicker
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

    // Reaction state
    var activeReactions by remember { mutableStateOf<List<DisplayReaction>>(emptyList()) }
    var reactionIdCounter by remember { mutableLongStateOf(0L) }
    var lastReactionSentTime by remember { mutableLongStateOf(0L) }

    // Collect incoming reactions and auto-remove after 3s
    LaunchedEffect(Unit) {
        onlineRepository.reactions.collect { reaction ->
            val id = reactionIdCounter++
            val display = DisplayReaction(
                id = id,
                senderId = reaction.senderId,
                senderName = reaction.senderName,
                emoji = reaction.reaction.emoji
            )
            activeReactions = activeReactions + display
            // Launch separate coroutine so the collector isn't blocked
            scope.launch {
                delay(3000)
                activeReactions = activeReactions.filter { it.id != id }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameBoardContent(
            viewModel = viewModel,
            headerOverlay = {
                if (!isQuitting) {
                    ConnectionBanner(connectionState = onlineRepository.connectionState)

                    // Reconnect countdown banners for disconnected players
                    ReconnectCountdownBanners(reconnectCountdowns)
                }
            },
            floatingActionButton = {
                if (!isQuitting) {
                    ReactionPicker(
                        onReaction = { reaction ->
                            val now = currentTimeMillis()
                            if (now - lastReactionSentTime >= 2000L) {
                                lastReactionSentTime = now
                                scope.launch { onlineRepository.sendReaction(reaction) }
                            }
                        }
                    )
                }
            }
        )

        // Floating reactions overlay (pass-through for pointer events)
        FloatingReactions(
            activeReactions = activeReactions,
            modifier = Modifier.fillMaxSize()
        )
    }
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
