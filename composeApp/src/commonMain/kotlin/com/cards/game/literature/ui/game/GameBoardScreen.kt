package com.cards.game.literature.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import kotlinx.coroutines.delay
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.preferences.TutorialPrefs
import com.cards.game.literature.ui.game.tutorial.TutorialOverlay
import com.cards.game.literature.ui.game.tutorial.TutorialState
import com.cards.game.literature.ui.game.tutorial.TutorialStep
import com.cards.game.literature.ui.game.tutorial.rememberTutorialState
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.viewmodel.GameUiState
import com.cards.game.literature.viewmodel.GameViewModel
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameBoardScreen(
    playerName: String,
    playerCount: Int,
    onGameEnd: () -> Unit,
    onQuit: () -> Unit,
    viewModel: GameViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showQuitDialog by remember { mutableStateOf(false) }

    BackHandler { showQuitDialog = true }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text(stringResource(Res.string.dialog_quit_game_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(Res.string.dialog_quit_game_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        onQuit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(Res.string.button_quit))
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text(stringResource(Res.string.button_keep_playing))
                }
            }
        )
    }

    // Start game only if it hasn't started yet. Using Unit as key would re-run
    // on every Activity recreation (e.g. theme change), reshuffling the cards.
    LaunchedEffect(Unit) {
        if (uiState.phase == GamePhase.WAITING) {
            viewModel.startGame(playerName, playerCount)
        }
    }

    // Navigate to result when game ends
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == GamePhase.FINISHED) {
            onGameEnd()
        }
    }

    val isFirstGame = remember { !TutorialPrefs.isFirstGameCompleted() }
    val tutorialState = rememberTutorialState(isFirstGame)

    // Mark tutorial complete when it finishes
    LaunchedEffect(tutorialState.isActive) {
        if (isFirstGame && !tutorialState.isActive) {
            TutorialPrefs.markFirstGameCompleted()
        }
    }

    GameBoardContent(viewModel = viewModel, tutorialState = tutorialState)
}

@Composable
fun GameBoardContent(
    viewModel: GameViewModel,
    headerOverlay: @Composable () -> Unit = {},
    tutorialState: TutorialState? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val gameLog by viewModel.gameLog.collectAsState()

    var showAskSheet by remember { mutableStateOf(false) }
    var showClaimSheet by remember { mutableStateOf(false) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var askSuit by remember { mutableStateOf<Suit?>(null) }
    var askIsLow by remember { mutableStateOf<Boolean?>(null) }
    // var selectedCard by remember { mutableStateOf<Card?>(null) } // TODO: future use
    var selectedTab by remember { mutableStateOf(GameTab.TABLE) }
    var previouslyMyTurn by remember { mutableStateOf(false) }

    // Clear suit selection if the selected half suit is claimed OR the player lost all its cards
    LaunchedEffect(uiState.halfSuitStatuses, uiState.myHandByHalfSuit) {
        val suit = askSuit
        val isLow = askIsLow
        if (suit != null && isLow != null) {
            val selectedHalfSuit = when (suit) {
                Suit.SPADES -> if (isLow) HalfSuit.SPADES_LOW else HalfSuit.SPADES_HIGH
                Suit.HEARTS -> if (isLow) HalfSuit.HEARTS_LOW else HalfSuit.HEARTS_HIGH
                Suit.DIAMONDS -> if (isLow) HalfSuit.DIAMONDS_LOW else HalfSuit.DIAMONDS_HIGH
                Suit.CLUBS -> if (isLow) HalfSuit.CLUBS_LOW else HalfSuit.CLUBS_HIGH
            }
            val isClaimed = uiState.halfSuitStatuses.any { it.halfSuit == selectedHalfSuit && it.claimedByTeamId != null }
            val playerHasCards = uiState.myHandByHalfSuit.containsKey(selectedHalfSuit)
            if (isClaimed || !playerHasCards) {
                askSuit = null
                askIsLow = null
            }
        }
    }

    // Auto-switch to Hand tab when it becomes the player's turn (suppressed during tutorial)
    LaunchedEffect(uiState.isMyTurn, tutorialState?.isActive) {
        if (uiState.isMyTurn && !previouslyMyTurn && tutorialState?.isActive != true) {
            selectedTab = GameTab.HAND
        }
        previouslyMyTurn = uiState.isMyTurn
    }

    // Error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Persistent header: ScoreBar + [banners] + TurnIndicatorBanner
                PersistentHeader(
                    uiState = uiState,
                    headerOverlay = headerOverlay,
                    onHelpClick = { showHelpSheet = true },
                    tutorialState = tutorialState
                )

                // Tab content area
                AnimatedContent(
                    targetState = selectedTab,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        (slideInHorizontally { it * dir } + fadeIn(tween(180))) togetherWith
                                (slideOutHorizontally { -it * dir } + fadeOut(tween(180)))
                    },
                    label = "TabContent"
                ) { tab ->
                    when (tab) {
                        GameTab.TABLE -> TableTab(
                            uiState = uiState,
                            tutorialState = tutorialState
                        )
                        GameTab.HAND -> HandTab(
                            uiState = uiState,
                            tutorialState = tutorialState
                        )
                    }
                }

                // Last event strip
                LastEventStrip(events = gameLog)

                // Bottom NavigationBar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    windowInsets = WindowInsets()
                ) {
                    GameTab.entries.forEach { tab ->
                        val tabLabel = stringResource(tab.labelRes)
                        NavigationBarItem(
                            modifier = if (tab == GameTab.HAND) {
                                Modifier.onGloballyPositioned { coords ->
                                    val rect = coords.boundsInRoot()
                                    tutorialState?.reportBounds(TutorialStep.HAND_TAB, rect)
                                }
                            } else Modifier,
                            icon = { Icon(tab.icon, contentDescription = tabLabel) },
                            label = { Text(tabLabel, style = MaterialTheme.typography.bodyLarge) },
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                                if (tab == GameTab.HAND
                                    && tutorialState?.currentStep == TutorialStep.HAND_TAB
                                ) {
                                    tutorialState.advance()
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Pinned action buttons
                ActionButtons(
                    isMyTurn = uiState.isMyTurn,
                    onAskCard = { showAskSheet = true },
                    onClaimDeck = { showClaimSheet = true },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .onGloballyPositioned { coords ->
                            tutorialState?.reportBounds(TutorialStep.ACTION_BUTTONS, coords.boundsInRoot())
                        }
                )
            }
        }

        // Tutorial overlay on top of everything
        if (tutorialState?.isActive == true) {
            TutorialOverlay(state = tutorialState)
        }
    }

    // Ask bottom sheet
    if (showAskSheet) {
        AskBottomSheet(
            myHandByHalfSuit = uiState.myHandByHalfSuit,
            opponents = uiState.opponents,
            initialSuit = askSuit,
            initialIsLow = askIsLow,
            onSuitSelected = { askSuit = it },
            onIsLowSelected = { askIsLow = it },
            onConfirm = { targetId, cards ->
                showAskSheet = false
                viewModel.askCards(targetId, cards)
            },
            onDismiss = { showAskSheet = false }
        )
    }

    // How to Play bottom sheet
    if (showHelpSheet) {
        HowToPlaySheet(onDismiss = { showHelpSheet = false })
    }

    // Claim bottom sheet
    if (showClaimSheet) {
        ClaimBottomSheet(
            myPlayerId = uiState.myPlayerId,
            myHand = uiState.myHand,
            myHalfSuits = uiState.myHandByHalfSuit.keys,
            teammates = uiState.teammates,
            halfSuitStatuses = uiState.halfSuitStatuses,
            onConfirm = { declaration ->
                showClaimSheet = false
                viewModel.claimDeck(declaration)
            },
            onDismiss = { showClaimSheet = false }
        )
    }
}

// ─── Last Event Strip ────────────────────────────────────────────────────────

private data class StripMessage(
    val indicator: String,
    val indicatorColor: Color,
    val text: String
)

/**
 * Consolidate raw game events into readable strip messages.
 * - Groups consecutive successful asks by the same asker into one line.
 * - Returns at most [limit] messages.
 * - Format functions are passed in from the composable context to avoid hardcoded strings.
 */
private fun consolidateEvents(
    events: List<GameEvent>,
    fmtGot: (askerName: String, cards: String, targetName: String) -> String,
    fmtDenied: (askerName: String, targetName: String, cardName: String) -> String,
    fmtClaimedOk: (claimerName: String, halfSuit: String) -> String,
    fmtClaimedBad: (claimerName: String, halfSuit: String) -> String,
    textGameOver: String,
    fmtTimedOut: (playerName: String) -> String,
    limit: Int = 5
): List<StripMessage> {
    val messages = mutableListOf<StripMessage>()
    var i = 0
    while (i < events.size) {
        when (val event = events[i]) {
            is GameEvent.CardAsked -> {
                if (event.success) {
                    // Group consecutive successful asks from the same batch (same click).
                    // Events from a single submitMultiAsk share the same non-null batchId.
                    val group = mutableListOf(event)
                    val batch = event.batchId
                    if (batch != null) {
                        while (i + 1 < events.size) {
                            val next = events[i + 1]
                            if (next is GameEvent.CardAsked && next.success
                                && next.batchId == batch
                            ) {
                                group.add(next)
                                i++
                            } else break
                        }
                    }
                    val cards = group.joinToString(", ") { it.card.displayName }
                    messages.add(StripMessage("✓", LightGreen, fmtGot(event.askerName, cards, event.targetName)))
                } else {
                    messages.add(StripMessage(
                        "✗", CardRed,
                        fmtDenied(event.askerName, event.targetName, event.card.displayName)
                    ))
                }
            }
            is GameEvent.DeckClaimed -> {
                if (event.correct) {
                    messages.add(StripMessage(
                        "✓", LightGreen,
                        fmtClaimedOk(event.claimerName, event.halfSuit.displayName)
                    ))
                } else {
                    messages.add(StripMessage(
                        "✗", CardRed,
                        fmtClaimedBad(event.claimerName, event.halfSuit.displayName)
                    ))
                }
            }
            is GameEvent.GameEnded -> {
                messages.add(StripMessage("★", GoldAccent, textGameOver))
            }
            is GameEvent.TurnTimedOut -> {
                messages.add(StripMessage("⏱", CardRed, fmtTimedOut(event.playerName)))
            }
            else -> {}
        }
        i++
    }
    return messages.takeLast(limit)
}

/**
 * Build an AnnotatedString that colors suit symbols for visibility:
 * ♥♦ → red, ♠♣ → bright [brightSuitColor] so they pop on dark backgrounds.
 */
private fun styleSuitSymbols(text: String, brightSuitColor: Color): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        for (char in text) {
            when (char) {
                '♥', '♦' -> withStyle(SpanStyle(color = CardRed, fontWeight = FontWeight.Bold)) { append(char) }
                '♠', '♣' -> withStyle(SpanStyle(color = brightSuitColor, fontWeight = FontWeight.Bold)) { append(char) }
                else -> append(char)
            }
        }
    }
}

@Composable
private fun StripEntry(message: StripMessage) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    // Bright color for ♠♣ — white in dark mode for max contrast, dark in light mode
    val darkSuitColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
        Color.White else Color(0xFF1C1B1F)
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            message.indicator,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = message.indicatorColor
        )
        Text(
            text = styleSuitSymbols(message.text, darkSuitColor),
            style = MaterialTheme.typography.bodyLarge,
            color = onSurface
        )
    }
}

@Composable
private fun LastEventStrip(events: List<GameEvent>) {
    // Every ask emits CardAsked + TurnChanged (even when same player keeps turn).
    // "Current turn" = all events since the last TurnChanged that pointed to a DIFFERENT player.
    val lastTurnChange = events.lastOrNull { it is GameEvent.TurnChanged } as? GameEvent.TurnChanged
        ?: return
    val currentPlayerId = lastTurnChange.newPlayerId

    // Find the last TurnChanged where a DIFFERENT player got the turn
    val lastOtherTurnIdx = events.indexOfLast { event ->
        event is GameEvent.TurnChanged && event.newPlayerId != currentPlayerId
    }

    // Current player's turn started at the first TurnChanged(currentPlayer) after lastOtherTurnIdx
    val turnStartIdx = if (lastOtherTurnIdx >= 0) {
        val slice = events.subList(lastOtherTurnIdx + 1, events.size)
        val firstCurrent = slice.indexOfFirst { event ->
            event is GameEvent.TurnChanged && event.newPlayerId == currentPlayerId
        }
        if (firstCurrent >= 0) lastOtherTurnIdx + 1 + firstCurrent + 1 else lastOtherTurnIdx + 1
    } else 0

    val currentTurnEvents = events.drop(turnStartIdx)
        .filterNot { it is GameEvent.TurnChanged || it is GameEvent.GameStarted }

    // If the current player hasn't acted yet, show the previous player's last batch
    // of events as context. Events with the same batchId are from a single click.
    // Once the current player acts, show only their events.
    val displayEvents = if (currentTurnEvents.isNotEmpty()) {
        currentTurnEvents
    } else {
        val prior = events.take(turnStartIdx)
            .filter { it !is GameEvent.TurnChanged && it !is GameEvent.GameStarted }
        if (prior.isEmpty()) emptyList()
        else {
            val lastEvent = prior.last()
            val lastBatchId = (lastEvent as? GameEvent.CardAsked)?.batchId
            if (lastBatchId != null) {
                // Collect all events from the same batch
                prior.filter { it is GameEvent.CardAsked && it.batchId == lastBatchId }
            } else {
                listOf(lastEvent)
            }
        }
    }

    if (displayEvents.isEmpty()) return

    // Pre-retrieve format strings in composable context
    val fmtGotCard = stringResource(Res.string.game_log_got_card)
    val fmtAskedNoStrip = stringResource(Res.string.game_log_asked_no_strip)
    val fmtClaimedOk = stringResource(Res.string.game_log_claimed_correctly)
    val fmtClaimedBad = stringResource(Res.string.game_log_claimed_incorrectly)
    val textGameOver = stringResource(Res.string.game_log_game_over)
    val fmtTimedOut = stringResource(Res.string.game_log_timed_out)

    val messages = consolidateEvents(
        events = displayEvents,
        fmtGot = { a, c, t -> fmtGotCard.format(a, c, t) },
        fmtDenied = { a, t, c -> fmtAskedNoStrip.format(a, t, c) },
        fmtClaimedOk = { c, h -> fmtClaimedOk.format(c, h) },
        fmtClaimedBad = { c, h -> fmtClaimedBad.format(c, h) },
        textGameOver = textGameOver,
        fmtTimedOut = { p -> fmtTimedOut.format(p) }
    )
    if (messages.isEmpty()) return

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            messages.forEach { msg -> StripEntry(msg) }
        }
    }
}

@Composable
private fun PersistentHeader(
    uiState: GameUiState,
    headerOverlay: @Composable () -> Unit = {},
    onHelpClick: () -> Unit = {},
    tutorialState: TutorialState? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.onGloballyPositioned { coords ->
            tutorialState?.reportBounds(TutorialStep.SCORE_BAR, coords.boundsInRoot())
        }) {
            ScoreBar(
                myTeamScore = uiState.myTeamScore,
                opponentTeamScore = uiState.opponentTeamScore
            )
        }
        headerOverlay()
        Box(modifier = Modifier.onGloballyPositioned { coords ->
            tutorialState?.reportBounds(TutorialStep.TURN_BANNER, coords.boundsInRoot())
        }) {
            TurnIndicatorBanner(
                uiState = uiState,
                onHelpClick = onHelpClick,
                timerPaused = tutorialState?.isActive == true
            )
        }
    }
}

@Composable
private fun TurnIndicatorBanner(
    uiState: GameUiState,
    onHelpClick: () -> Unit = {},
    timerPaused: Boolean = false
) {
    // Local 60s countdown, reset on every game state change (ask, claim, turn change)
    var secondsRemaining by remember { mutableStateOf(60) }

    // Use activePlayerId + myHand size + scores as a composite key that changes on every action
    val timerKey = "${uiState.activePlayerId}_${uiState.myHand.size}_${uiState.myTeamScore}_${uiState.opponentTeamScore}"

    LaunchedEffect(timerKey, timerPaused) {
        secondsRemaining = 60
        while (secondsRemaining > 0 && !timerPaused) {
            delay(1000L)
            secondsRemaining--
        }
    }

    AnimatedVisibility(visible = uiState.phase == GamePhase.IN_PROGRESS) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (uiState.isMyTurn) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isMyTurn) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(Res.string.game_your_turn),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (uiState.isOnline && secondsRemaining <= 15) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(Res.string.game_timer_seconds, secondsRemaining),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (secondsRemaining <= 10) CardRed else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else if (uiState.isBotThinking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val botDesc = stringResource(Res.string.cd_bot_thinking)
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp).semantics { contentDescription = botDesc },
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(Res.string.game_bot_thinking, uiState.activePlayerName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        stringResource(Res.string.game_player_turn, uiState.activePlayerName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.isOnline && secondsRemaining <= 15) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(Res.string.game_timer_seconds, secondsRemaining),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (secondsRemaining <= 10) CardRed
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Help icon
            val helpDesc = stringResource(Res.string.help_button_description)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(22.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .clickable(onClick = onHelpClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() })
                    .semantics { contentDescription = helpDesc },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "?",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
