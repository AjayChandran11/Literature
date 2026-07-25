package com.cards.game.literature.ui.lobby

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.analytics.Analytics
import com.cards.game.literature.analytics.AnalyticsEvent
import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.bot.BotPersonalities
import com.cards.game.literature.deeplink.InviteLink
import com.cards.game.literature.repository.PlayerConnectionEvent
import com.cards.game.literature.share.Sharer
import com.cards.game.literature.ui.common.ConnectionBanner
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import com.cards.game.literature.ui.common.WindowSize.isCompactHeight
import com.cards.game.literature.ui.common.WindowSize.useSideBySide
import com.cards.game.literature.viewmodel.WaitingRoomPlayer
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
    var selectedDifficulty by remember { mutableStateOf(BotDifficulty.MEDIUM) }
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

    // Guarded leave shared by the bottom button (the dialog has its own inline copy that also
    // dismisses itself first).
    val onLeaveClicked: () -> Unit = {
        if (!isLeaving) {
            isLeaving = true
            viewModel.leaveRoom()
            onLeave()
        }
    }
    val errorToShow = when {
        uiState.isStartGameTimedOut -> startGameTimeoutMsg
        else -> uiState.errorMessage
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
        if (!isLeaving) {
            ConnectionBanner(
                connectionState = viewModel.connectionState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        val windowInfo = currentWindowAdaptiveInfo()
        val isCompact = windowInfo.isCompactHeight
        val sideBySide = windowInfo.useSideBySide

        if (sideBySide) {
            // Landscape / tablet / unfolded foldable: two panes. The players list gets its own
            // full-height, scrollable column instead of fighting the fixed chrome for a sliver of
            // vertical space.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = if (isCompact) 8.dp else 16.dp)
            ) {
                // LEFT: a scrollable body + a pinned Start/Leave footer. On a short pane (phone
                // landscape) the body scrolls and the actions stay visible; on a tall pane (tablet)
                // the body sits at the top and the weight pushes the footer to the bottom, so the
                // controls never float mid-pane above a void.
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RoomCodeCard(uiState.roomCode)
                        Spacer(modifier = Modifier.height(12.dp))
                        InviteButton(uiState.roomCode)
                        Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 20.dp))
                        if (uiState.isHost) {
                            HostSetupControls(
                                players = uiState.players,
                                fillWithBots = fillWithBots,
                                onFillWithBots = { fillWithBots = it },
                                selectedDifficulty = selectedDifficulty,
                                onDifficulty = { selectedDifficulty = it }
                            )
                        } else {
                            WaitingForHostText()
                        }
                    }
                    if (uiState.isHost) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StartGameButton(
                            players = uiState.players,
                            targetPlayerCount = uiState.targetPlayerCount,
                            fillWithBots = fillWithBots,
                            isStarting = uiState.isStarting,
                            onStartGame = { viewModel.startGame(fillWithBots, selectedDifficulty) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LeaveRoomButton(onLeave = onLeaveClicked)
                    errorToShow?.let { ErrorSection(it, onClearError = viewModel::clearError) }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // RIGHT: the players list gets the full pane height.
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                ) {
                    PlayersCountHeader(uiState.players.size, uiState.targetPlayerCount)
                    Spacer(modifier = Modifier.height(12.dp))
                    PlayerList(
                        players = uiState.players,
                        myPlayerId = uiState.myPlayerId,
                        onSwitchTeam = viewModel::switchTeam,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Portrait: the original single column (plenty of vertical space here).
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isCompact) 16.dp else 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 32.dp))

                Text(
                    text = stringResource(Res.string.waiting_room_title),
                    style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

                RoomCodeCard(uiState.roomCode)
                Spacer(modifier = Modifier.height(12.dp))
                InviteButton(uiState.roomCode)

                Spacer(modifier = Modifier.height(24.dp))

                PlayersCountHeader(uiState.players.size, uiState.targetPlayerCount)
                Spacer(modifier = Modifier.height(12.dp))

                PlayerList(
                    players = uiState.players,
                    myPlayerId = uiState.myPlayerId,
                    onSwitchTeam = viewModel::switchTeam,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isHost) {
                    HostSetupControls(
                        players = uiState.players,
                        fillWithBots = fillWithBots,
                        onFillWithBots = { fillWithBots = it },
                        selectedDifficulty = selectedDifficulty,
                        onDifficulty = { selectedDifficulty = it }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StartGameButton(
                        players = uiState.players,
                        targetPlayerCount = uiState.targetPlayerCount,
                        fillWithBots = fillWithBots,
                        isStarting = uiState.isStarting,
                        onStartGame = { viewModel.startGame(fillWithBots, selectedDifficulty) }
                    )
                } else {
                    WaitingForHostText()
                }

                Spacer(modifier = Modifier.height(12.dp))
                LeaveRoomButton(onLeave = onLeaveClicked)
                errorToShow?.let { ErrorSection(it, onClearError = viewModel::clearError) }
            }
        }
    } // Box
    } // Scaffold
}

// ─── Shared building blocks (used by both the portrait column and the landscape panes) ─────────

@Composable
private fun RoomCodeCard(roomCode: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.padding(8.dp)
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
                text = roomCode,
                style = MaterialTheme.typography.displaySmall,
                // The code mixes caps and digits; Playfair's old-style figures render the
                // numbers small and low next to the caps. Monospace gives every glyph the
                // same metrics, so the code reads evenly — and like the code it is.
                fontFamily = FontFamily.Monospace,
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
}

@Composable
private fun InviteButton(roomCode: String, modifier: Modifier = Modifier) {
    // Invite friends via the system share sheet (deep-link room invite).
    val inviteText = stringResource(
        Res.string.invite_share_text,
        roomCode,
        InviteLink.forRoom(roomCode)
    )
    OutlinedButton(
        onClick = {
            Analytics.log(AnalyticsEvent.InviteShared(surface = "waiting_room"))
            Sharer.shareText(inviteText)
        },
        enabled = roomCode.isNotBlank(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.waiting_room_invite),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlayersCountHeader(count: Int, target: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.waiting_room_players_count, count, target),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@Composable
private fun PlayerList(
    players: List<WaitingRoomPlayer>,
    myPlayerId: String,
    onSwitchTeam: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(players, key = { it.id }) { player ->
            // Same spring language as the card hand: joins fade+settle in,
            // leavers fade out, team switches glide to their new slot.
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(
                        fadeInSpec = tween(300),
                        fadeOutSpec = tween(300),
                        placementSpec = spring<IntOffset>(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
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

                    if (player.isBot) {
                        Text(
                            text = BotPersonalities.emojiFor(player.name),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

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

                    if (player.id == myPlayerId) {
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = onSwitchTeam,
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
}

/** Host setup that lives in the scrollable body: the fill-with-bots toggle, difficulty selector,
 *  and the uneven-teams warning. The Start button is separate ([StartGameButton]) so it can be
 *  pinned to the bottom of the landscape pane. */
@Composable
private fun HostSetupControls(
    players: List<WaitingRoomPlayer>,
    fillWithBots: Boolean,
    onFillWithBots: (Boolean) -> Unit,
    selectedDifficulty: BotDifficulty,
    onDifficulty: (BotDifficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    val team1Count = players.count { it.teamId == "team_1" }
    val team2Count = players.count { it.teamId == "team_2" }
    val teamsUneven = !fillWithBots && team1Count != team2Count

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Checkbox(
                checked = fillWithBots,
                onCheckedChange = onFillWithBots,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.waiting_room_fill_bots),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Bot difficulty selector — shown when filling with bots
        if (fillWithBots) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.game_setup_difficulty_label),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val difficultyLabels = mapOf(
                    BotDifficulty.EASY to Pair(stringResource(Res.string.difficulty_easy), stringResource(Res.string.difficulty_easy_desc)),
                    BotDifficulty.MEDIUM to Pair(stringResource(Res.string.difficulty_medium), stringResource(Res.string.difficulty_medium_desc)),
                    BotDifficulty.HARD to Pair(stringResource(Res.string.difficulty_hard), stringResource(Res.string.difficulty_hard_desc))
                )
                BotDifficulty.entries.forEach { difficulty ->
                    val isSelected = selectedDifficulty == difficulty
                    val (label, desc) = difficultyLabels[difficulty] ?: Pair(difficulty.label, "")
                    val primary = MaterialTheme.colorScheme.primary
                    val secondary = MaterialTheme.colorScheme.secondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) primary else primary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onDifficulty(difficulty) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
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
    }
}

@Composable
private fun StartGameButton(
    players: List<WaitingRoomPlayer>,
    targetPlayerCount: Int,
    fillWithBots: Boolean,
    isStarting: Boolean,
    onStartGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val team1Count = players.count { it.teamId == "team_1" }
    val team2Count = players.count { it.teamId == "team_2" }
    val teamsUneven = !fillWithBots && team1Count != team2Count

    Button(
        onClick = onStartGame,
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = !isStarting && !teamsUneven && (fillWithBots || players.size == targetPlayerCount)
    ) {
        if (isStarting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(stringResource(Res.string.button_start_game), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WaitingForHostText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.waiting_room_waiting_for_host),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
private fun LeaveRoomButton(onLeave: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onLeave, modifier = modifier) {
        Text(stringResource(Res.string.button_leave_room), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ErrorSection(error: String, onClearError: () -> Unit) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = error,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    LaunchedEffect(error) {
        kotlinx.coroutines.delay(3000)
        onClearError()
    }
}
