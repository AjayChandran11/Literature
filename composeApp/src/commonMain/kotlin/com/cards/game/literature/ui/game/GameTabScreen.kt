package com.cards.game.literature.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.*
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.ui.game.tutorial.TutorialState
import com.cards.game.literature.ui.game.tutorial.TutorialStep
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.viewmodel.GameUiState
import com.cards.game.literature.viewmodel.PlayerInfo
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class GameTab(val labelRes: StringResource, val icon: ImageVector) {
    TABLE(Res.string.tab_table, Icons.Filled.GridView),
    HAND(Res.string.tab_hand, Icons.Filled.PanTool),
}

// enum class LogFilter { ALL, ASKS, CLAIMS }  // Only used by LogTab

@Composable
fun TableTab(uiState: GameUiState, tutorialState: TutorialState? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Opponents section — takes equal space
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            SectionLabel(stringResource(Res.string.label_opponents_section))
            OpponentRow(
                opponents = uiState.opponents,
                modifier = Modifier.onGloballyPositioned { coords ->
                    tutorialState?.reportBounds(TutorialStep.PLAYERS, coords.boundsInRoot())
                }
            )
        }

        // Teammates section — takes equal space
        if (uiState.teammates.isNotEmpty()) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                SectionLabel(stringResource(Res.string.label_teammates_section))
                TeammateRow(teammates = uiState.teammates)
            }
        }

        // Half-suits pinned at the bottom
        Column(
            modifier = Modifier.onGloballyPositioned { coords ->
                tutorialState?.reportBounds(TutorialStep.HALF_SUITS, coords.boundsInRoot())
            }
        ) {
            SectionLabel(stringResource(Res.string.label_half_suits_section))
            DeckTracker(
                statuses = uiState.halfSuitStatuses,
                myTeamId = uiState.myTeamId
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun HandTab(uiState: GameUiState, tutorialState: TutorialState? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            stringResource(Res.string.label_your_hand),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (uiState.myHandByHalfSuit.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.hand_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(Res.string.hand_empty_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            CardHand(
                handByHalfSuit = uiState.myHandByHalfSuit,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coords ->
                        tutorialState?.reportBounds(TutorialStep.YOUR_HAND, coords.boundsInRoot())
                    }
            )
        }
    }
}

/*
@Composable
fun LogTab(events: List<GameEvent>) {
    var filter by rememberSaveable { mutableStateOf(LogFilter.ALL) }
    val listState = rememberLazyListState()

    val displayEvents = events.filterNot { it is GameEvent.TurnChanged || it is GameEvent.GameStarted }
    val filteredEvents = when (filter) {
        LogFilter.ALL -> displayEvents
        LogFilter.ASKS -> displayEvents.filterIsInstance<GameEvent.CardAsked>()
        LogFilter.CLAIMS -> displayEvents.filterIsInstance<GameEvent.DeckClaimed>()
    }

    LaunchedEffect(filteredEvents.size) {
        if (filteredEvents.isNotEmpty()) listState.animateScrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            "GAME LOG",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (filteredEvents.isEmpty()) {
            Text(
                "No events yet.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredEvents.reversed()) { event ->
                    GameLogEntry(event)
                }
            }
        }
    }
}
*/

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}

// ─── Previews ────────────────────────────────────────────────────────────────

private val previewOpponents = listOf(
    PlayerInfo("o1", "Rahul", cardCount = 7, isActive = true, isCurrentTurn = true),
    PlayerInfo("o2", "Priya", cardCount = 0, isActive = false, isCurrentTurn = false),
    PlayerInfo("o3", "Amit", cardCount = 4, isActive = true, isCurrentTurn = false),
)

private val previewTeammates = listOf(
    PlayerInfo("t1", "Sneha", cardCount = 5, isActive = true, isCurrentTurn = false),
    PlayerInfo("t2", "Karan", cardCount = 0, isActive = false, isCurrentTurn = false),
)

private val previewHandWithCards = mapOf(
    HalfSuit.SPADES_LOW to listOf(
        Card(Suit.SPADES, CardValue.TWO),
        Card(Suit.SPADES, CardValue.FOUR),
        Card(Suit.SPADES, CardValue.SIX),
    ),
    HalfSuit.HEARTS_HIGH to listOf(
        Card(Suit.HEARTS, CardValue.ACE),
        Card(Suit.HEARTS, CardValue.KING),
    ),
)

private val previewHalfSuitStatuses = HalfSuit.entries.map { hs ->
    HalfSuitStatus(halfSuit = hs, claimedByTeamId = if (hs == HalfSuit.CLUBS_LOW) "team_1" else null)
}

private val previewUiStateWithCards = GameUiState(
    isMyTurn = false,
    myHand = previewHandWithCards.values.flatten(),
    myHandByHalfSuit = previewHandWithCards,
    opponents = previewOpponents,
    teammates = previewTeammates,
    myTeamScore = 2,
    opponentTeamScore = 1,
    halfSuitStatuses = previewHalfSuitStatuses,
    phase = GamePhase.IN_PROGRESS,
    activePlayerName = "Rahul",
    activePlayerId = "o1",
    myTeamId = "team_1"
)

private val previewUiStateEmptyHand = previewUiStateWithCards.copy(
    myHand = emptyList(),
    myHandByHalfSuit = emptyMap(),
)

@Preview(name = "TableTab — with cards", showBackground = true)
@Composable
private fun PreviewTableTab() {
    LiteratureTheme {
        TableTab(uiState = previewUiStateWithCards)
    }
}

@Preview(name = "HandTab — with cards", showBackground = true)
@Composable
private fun PreviewHandTabWithCards() {
    LiteratureTheme {
        HandTab(uiState = previewUiStateWithCards)
    }
}

@Preview(name = "HandTab — empty hand", showBackground = true)
@Composable
private fun PreviewHandTabEmpty() {
    LiteratureTheme {
        HandTab(uiState = previewUiStateEmptyHand)
    }
}
