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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.analytics.Analytics
import com.cards.game.literature.analytics.AnalyticsEvent
import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.bot.BotPersonalities
import com.cards.game.literature.deeplink.InviteLink
import com.cards.game.literature.repository.PlayerConnectionEvent
import com.cards.game.literature.share.Sharer
import com.cards.game.literature.stats.StatsStore
import com.cards.game.literature.ui.game.HowToPlaySheet
import com.cards.game.literature.ui.common.ConnectionBanner
import com.cards.game.literature.ui.theme.LiteratureTheme
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
    var showHowToPlay by remember { mutableStateOf(false) }
    // Dismissible, and rememberSaveable so a uiMode/config recreation doesn't un-dismiss it.
    var primerDismissed by rememberSaveable { mutableStateOf(false) }
    // Primer targets a player who's never finished a game (e.g. a deep-link novice landing
    // cold). gamesPlayed also covers online-first players, who never mark the offline tutorial.
    val isNovice = remember { StatsStore.stats.value.gamesPlayed == 0 }
    val showPrimer = isNovice && !primerDismissed

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

    if (showHowToPlay) {
        HowToPlaySheet(onDismiss = { showHowToPlay = false })
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
                        if (showPrimer) {
                            WaitingRoomPrimer(
                                onHowToPlay = { showHowToPlay = true },
                                onDismiss = { primerDismissed = true }
                            )
                            Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 20.dp))
                        }
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
                // Every dp of fixed chrome here comes straight out of the players list
                // (the only weight(1f) child), so the gaps stay lean in portrait too.
                Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 12.dp))

                Text(
                    text = stringResource(Res.string.waiting_room_title),
                    style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))

                RoomCodeCard(uiState.roomCode)
                Spacer(modifier = Modifier.height(12.dp))
                InviteButton(uiState.roomCode)

                Spacer(modifier = Modifier.height(16.dp))

                PlayersCountHeader(uiState.players.size, uiState.targetPlayerCount)
                Spacer(modifier = Modifier.height(8.dp))

                // Portrait budget is tight: as a fixed sibling the primer starved the
                // weight(1f) list to zero height. Ride it inside the list's own scroll instead.
                PlayerList(
                    players = uiState.players,
                    myPlayerId = uiState.myPlayerId,
                    onSwitchTeam = viewModel::switchTeam,
                    modifier = Modifier.weight(1f),
                    header = if (showPrimer) {
                        {
                            WaitingRoomPrimer(
                                onHowToPlay = { showHowToPlay = true },
                                onDismiss = { primerDismissed = true }
                            )
                        }
                    } else null
                )

                Spacer(modifier = Modifier.height(12.dp))

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
    // No outer margin and a tighter vertical inset — the card is centred by its
    // parent, so the old padding(8) was pure air billed to the players list.
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
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
            // No "share this code with friends" caption — the labelled invite button sits
            // directly below and says the same thing, actionably. Dropping it keeps the code
            // itself the hero of the card and buys the players list back ~18dp in portrait.
        }
    }
}

/** Uniform height for both invite variants, so the block's vertical cost never changes. */
private val InviteRowHeight = 48.dp

@Composable
private fun InviteButton(
    roomCode: String,
    modifier: Modifier = Modifier,
    // Hoisted so previews can force either branch. The real check reads the PackageManager,
    // which a @Preview has no access to — left un-hoisted it always renders "not installed".
    // Resolved once in production: WhatsApp can't be installed while this screen is up.
    whatsAppAvailable: Boolean = remember { Sharer.isWhatsAppAvailable() },
) {
    // Deep-link room invite. WhatsApp-first: a direct WhatsApp CTA when it's installed (the
    // dominant channel here), with the system share sheet as the fallback.
    val inviteText = stringResource(
        Res.string.invite_share_text,
        roomCode,
        InviteLink.forRoom(roomCode)
    )
    val enabled = roomCode.isNotBlank()

    val shareViaSystem = {
        Analytics.log(AnalyticsEvent.InviteShared(surface = "waiting_room", channel = "system"))
        Sharer.shareText(inviteText)
    }

    if (!whatsAppAvailable) {
        // No WhatsApp installed — a single generic invite button.
        OutlinedButton(
            onClick = shareViaSystem,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
            modifier = modifier.height(InviteRowHeight)
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.waiting_room_invite),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        return
    }

    // WhatsApp installed. Two deliberate calls here:
    //
    // 1. One row, not a stacked Column: "other apps" is a trailing icon rather than a second
    //    full-height TextButton. Portrait is where this matters — PlayerList is the only
    //    weight(1f) child, so every dp spent here comes straight out of the players list.
    // 2. Tonal, NOT WhatsApp brand green. Saturated #25D366 only earns that much attention when
    //    it carries the actual WhatsApp mark and reads as recognition; with a generic Chat glyph
    //    it paid the full cost of standing out and collected none of the benefit — and its
    //    hand-picked on-green ink didn't adapt to the dark theme. Tonal keeps this obviously the
    //    screen's main action while leaving StartGameButton the only filled-primary button, so
    //    the hierarchy reads status -> invite -> start.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = {
                Analytics.log(AnalyticsEvent.InviteShared(surface = "waiting_room", channel = "whatsapp"))
                // Fall back to the sheet if WhatsApp vanished between check and tap.
                if (!Sharer.shareTextToWhatsApp(inviteText)) Sharer.shareText(inviteText)
            },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(InviteRowHeight)
        ) {
            // Chat, not Share — the trailing button is the share-sheet one, and two identical
            // share glyphs side by side read as the same action twice.
            Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.waiting_room_invite_whatsapp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        FilledTonalIconButton(
            onClick = shareViaSystem,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.size(InviteRowHeight)
        ) {
            Icon(
                Icons.Filled.Share,
                contentDescription = stringResource(Res.string.cd_invite_other_apps),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Invite previews ───────────────────────────────────────────────────────────────────────────
// InviteButton's real WhatsApp check reads the PackageManager, which a @Preview has no access to,
// so it would always render the "not installed" branch. `whatsAppAvailable` is a parameter purely
// so these can force both branches without hunting for a device that has WhatsApp installed.

@Preview(name = "Invite — WhatsApp installed", showBackground = true)
@Composable
private fun InviteButtonWhatsAppPreview() {
    LiteratureTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            InviteButton(roomCode = "KRA7QM", whatsAppAvailable = true)
        }
    }
}

@Preview(name = "Invite — no WhatsApp (fallback)", showBackground = true)
@Composable
private fun InviteButtonNoWhatsAppPreview() {
    LiteratureTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            InviteButton(roomCode = "KRA7QM", whatsAppAvailable = false)
        }
    }
}

@Preview(name = "Invite — blank code (disabled)", showBackground = true)
@Composable
private fun InviteButtonDisabledPreview() {
    LiteratureTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            InviteButton(roomCode = "", whatsAppAvailable = true)
        }
    }
}

private val previewPlayers = listOf(
    WaitingRoomPlayer("p1", "Ajay", "team1", isHost = true, isConnected = true),
    WaitingRoomPlayer("p2", "Divya", "team2", isHost = false, isConnected = true),
    WaitingRoomPlayer("p3", "Karthik", "team1", isHost = false, isConnected = true),
)

/**
 * The portrait hero block at a real phone size — title, room code, invite, players list — so the
 * invite row's vertical cost is visible against the list it competes with. PlayerList is the only
 * weight(1f) child on the real screen, so this is where space either goes or gets taken.
 */
@Composable
private fun WaitingRoomHeroPreviewBody(whatsAppAvailable: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.waiting_room_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        RoomCodeCard("KRA7QM")
        Spacer(modifier = Modifier.height(12.dp))
        InviteButton(roomCode = "KRA7QM", whatsAppAvailable = whatsAppAvailable)
        Spacer(modifier = Modifier.height(24.dp))
        PlayersCountHeader(previewPlayers.size, target = 6)
        Spacer(modifier = Modifier.height(12.dp))
        PlayerList(
            players = previewPlayers,
            myPlayerId = "p1",
            onSwitchTeam = {},
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(name = "Waiting room hero — phone, WhatsApp", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun WaitingRoomHeroPhonePreview() {
    LiteratureTheme { WaitingRoomHeroPreviewBody(whatsAppAvailable = true) }
}

@Preview(name = "Waiting room hero — phone, dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun WaitingRoomHeroDarkPreview() {
    LiteratureTheme(darkTheme = true) { WaitingRoomHeroPreviewBody(whatsAppAvailable = true) }
}

/** Short/compact phone — the case where the invite block used to squeeze the list hardest. */
@Preview(name = "Waiting room hero — short phone", showBackground = true, widthDp = 360, heightDp = 560)
@Composable
private fun WaitingRoomHeroShortPreview() {
    LiteratureTheme { WaitingRoomHeroPreviewBody(whatsAppAvailable = true) }
}

/** Narrow phone — checks the green label and the trailing icon still fit on one row. */
@Preview(name = "Waiting room hero — narrow (320dp)", showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun WaitingRoomHeroNarrowPreview() {
    LiteratureTheme { WaitingRoomHeroPreviewBody(whatsAppAvailable = true) }
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
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (header != null) {
            item { header() }
        }
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
                // Every row is pinned to the same 48dp height. Without this, my own
                // row grows taller than everyone else's: the Switch TextButton brings
                // Material's 40dp min height + 48dp touch-target inflation, while other
                // rows top out at the ~24dp name text — reading as uneven padding.
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
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
                        // Opt out of the 48dp touch-target inflation so the button's
                        // 40dp min height fits inside the shared 48dp row instead of
                        // stretching it; the row itself keeps the tap area generous.
                        CompositionLocalProvider(
                            LocalMinimumInteractiveComponentSize provides Dp.Unspecified
                        ) {
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

        // Bot difficulty selector — shown when filling with bots. No caption row: the
        // Easy/Medium/Hard chips directly under the bots checkbox are self-describing,
        // and in portrait every fixed row here is paid for by the players list. (The
        // Game Setup sheet keeps its caption — it has the room.)
        if (fillWithBots) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val difficultyLabels = mapOf(
                    BotDifficulty.EASY to stringResource(Res.string.difficulty_easy),
                    BotDifficulty.MEDIUM to stringResource(Res.string.difficulty_medium),
                    BotDifficulty.HARD to stringResource(Res.string.difficulty_hard)
                )
                BotDifficulty.entries.forEach { difficulty ->
                    val isSelected = selectedDifficulty == difficulty
                    val label = difficultyLabels[difficulty] ?: difficulty.label
                    val primary = MaterialTheme.colorScheme.primary

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
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Single line — the "Forgiving/Balanced/Expert" blurbs live in the
                        // Game Setup sheet; here they doubled the chip height for words the
                        // host has already seen.
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) primary else MaterialTheme.colorScheme.onSurface
                        )
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

/** A dismissible "what is this game" card for first-timers waiting in the room — the invited
 *  novice who lands here cold via a deep link. Reuses the objective/teams help copy and opens
 *  the full [HowToPlaySheet] for the rest. Shown only when [gamesPlayed] is 0. */
@Composable
private fun WaitingRoomPrimer(
    onHowToPlay: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.waiting_room_primer_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.cd_dismiss),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Text(
                text = stringResource(Res.string.help_objective_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.help_teams_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onHowToPlay,
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.waiting_room_primer_cta),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
