package com.cards.game.literature.ui.game

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
    viewModel: GameViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val gameLog by viewModel.gameLog.collectAsState()

    var showAskSheet by remember { mutableStateOf(false) }
    var showClaimSheet by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<Card?>(null) }
    var selectedTab by remember { mutableStateOf(GameTab.TABLE) }
    var previouslyMyTurn by remember { mutableStateOf(false) }

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
                    GameTab.LOG -> LogTab(events = gameLog)
                }
            }

            // Bottom NavigationBar
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                GameTab.entries.forEach { tab ->
                    NavigationBarItem(
                        icon = { Text(tab.symbol, fontSize = 18.sp) },
                        label = { Text(tab.label) },
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
            myHalfSuits = uiState.myHandByHalfSuit.keys,
            opponents = uiState.opponents,
            onConfirm = { targetId, card ->
                showAskSheet = false
                viewModel.askCard(targetId, card)
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
