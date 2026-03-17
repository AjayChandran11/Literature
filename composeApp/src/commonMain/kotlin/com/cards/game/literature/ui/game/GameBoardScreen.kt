package com.cards.game.literature.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.GameUiState
import com.cards.game.literature.viewmodel.GameViewModel
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
            title = { Text("Quit Game?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to quit? Your progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        showQuitDialog = false
                        onQuit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Quit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuitDialog = false }) {
                    Text("Keep Playing")
                }
            }
        )
    }

    // Start game on first composition
    LaunchedEffect(Unit) {
        viewModel.startGame(playerName, playerCount)
    }

    // Navigate to result when game ends
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == GamePhase.FINISHED) {
            onGameEnd()
        }
    }

    GameBoardContent(viewModel = viewModel)
}

@Composable
fun GameBoardContent(
    viewModel: GameViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val gameLog by viewModel.gameLog.collectAsState()

    var showAskSheet by remember { mutableStateOf(false) }
    var showClaimSheet by remember { mutableStateOf(false) }
    var askSuit by remember { mutableStateOf<Suit?>(null) }
    var askIsLow by remember { mutableStateOf<Boolean?>(null) }
    var selectedCard by remember { mutableStateOf<Card?>(null) }
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

    // Auto-switch to Hand tab when it becomes the player's turn
    LaunchedEffect(uiState.isMyTurn) {
        if (uiState.isMyTurn && !previouslyMyTurn) selectedTab = GameTab.HAND
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Persistent header: ScoreBar + TurnIndicatorBanner
            PersistentHeader(uiState = uiState)

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
                    GameTab.TABLE -> TableTab(uiState = uiState)
                    GameTab.HAND -> HandTab(
                        uiState = uiState,
                        selectedCard = selectedCard,
                        onCardSelected = { selectedCard = it }
                    )
                    // GameTab.LOG -> LogTab(events = gameLog)
                }
            }

            // Last event strip
            LastEventStrip(events = gameLog)

            // Bottom NavigationBar
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                GameTab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = { Text(tab.symbol, fontSize = 16.sp) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldAccent,
                            selectedTextColor = GoldAccent,
                            indicatorColor = GoldAccent.copy(alpha = 0.15f),
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
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

    // If the current player hasn't acted yet (just received the turn), show the PREVIOUS player's
    // full session instead — apply the same session-start algorithm for the previous player.
    val displayEvents = if (currentTurnEvents.isNotEmpty()) {
        currentTurnEvents
    } else {
        val lastTurnIdx = events.indexOfLast { it is GameEvent.TurnChanged }
        val prevPlayerId = (events.getOrNull(lastOtherTurnIdx) as? GameEvent.TurnChanged)?.newPlayerId
        if (prevPlayerId != null && lastOtherTurnIdx >= 0) {
            // Find where the previous player's session started (search only up to lastTurnIdx)
            val truncated = events.subList(0, lastTurnIdx)
            val prevOtherIdx = truncated.indexOfLast { event ->
                event is GameEvent.TurnChanged && event.newPlayerId != prevPlayerId
            }
            val prevSessionStart = if (prevOtherIdx >= 0) {
                val slice = truncated.subList(prevOtherIdx + 1, truncated.size)
                val firstPrev = slice.indexOfFirst { event ->
                    event is GameEvent.TurnChanged && event.newPlayerId == prevPlayerId
                }
                if (firstPrev >= 0) prevOtherIdx + 1 + firstPrev + 1 else prevOtherIdx + 1
            } else 0
            truncated.drop(prevSessionStart)
                .filterNot { it is GameEvent.TurnChanged || it is GameEvent.GameStarted }
        } else emptyList()
    }

    if (displayEvents.isEmpty()) return

    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            displayEvents.forEach { event ->
                GameLogEntry(event = event, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PersistentHeader(uiState: GameUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ScoreBar(
            myTeamScore = uiState.myTeamScore,
            opponentTeamScore = uiState.opponentTeamScore
        )
        TurnIndicatorBanner(uiState = uiState)
    }
}

@Composable
private fun TurnIndicatorBanner(uiState: GameUiState) {
    AnimatedVisibility(visible = uiState.phase == GamePhase.IN_PROGRESS) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (uiState.isMyTurn) GoldAccent.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isMyTurn) {
                Text(
                    "\u2726 Your Turn!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAccent
                )
            } else if (uiState.isBotThinking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${uiState.activePlayerName} is thinking...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    "${uiState.activePlayerName}'s turn",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
