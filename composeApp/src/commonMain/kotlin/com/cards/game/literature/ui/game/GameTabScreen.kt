package com.cards.game.literature.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.cards.game.literature.ui.game.tutorial.TutorialState
import com.cards.game.literature.ui.game.tutorial.TutorialStep
import com.cards.game.literature.ui.theme.LightGreen
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

@Composable
fun TableTab(uiState: GameUiState, tutorialState: TutorialState? = null) {
    // The shared LastEventStrip (in the Scaffold's bottomBar) grows to 5 rows, which
    // shrinks this tab's available height. The old layout distributed the sections by
    // weight and never scrolled, so on a tall strip / small screen the fixed-size player
    // avatars and the half-suit tracker got squeezed — clipping the card-count text and
    // the tracker. Instead we let the content scroll: heightIn(min = maxHeight) plus
    // Arrangement.SpaceBetween makes it FILL the viewport when there's room and SCROLL
    // when there isn't. Fully fluid (driven by the real available height, no fixed sizes)
    // and consistent with the Hand tab and the tablet side-by-side panel.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Opponents
            Column {
                SectionLabel(stringResource(Res.string.label_opponents_section))
                OpponentRow(
                    opponents = uiState.opponents,
                    modifier = Modifier.onGloballyPositioned { coords ->
                        tutorialState?.reportBounds(TutorialStep.PLAYERS, coords.boundsInRoot())
                    }
                )
            }

            // Teammates
            if (uiState.teammates.isNotEmpty()) {
                Column {
                    SectionLabel(stringResource(Res.string.label_teammates_section))
                    TeammateRow(teammates = uiState.teammates)
                }
            }

            // Half-suits
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
}

@Composable
fun HandTab(uiState: GameUiState, tutorialState: TutorialState? = null) {
    // No bottom padding on the column: cards scroll flush to the event strip
    // instead of clipping 8dp above it (the last row's breathing room comes
    // from the list's own contentPadding below).
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp)
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
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coords ->
                        tutorialState?.reportBounds(TutorialStep.YOUR_HAND, coords.boundsInRoot())
                    }
            )
        }
    }
}


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

// ─── TableTab squeeze previews ───────────────────────────────────────────────
// These reproduce the real portrait screen budget — header on top, TableTab in the
// content slot (outlined so you can see its bounds), then the LastEventStrip + nav +
// action bars at the bottom — so you can verify the tab FILLS when roomy and SCROLLS
// (never clips the card counts / tracker) when the 5-row strip eats the space.

/** Mirrors LastEventStrip's surface + StripEntry rows so it consumes a realistic height. */
@Composable
private fun PreviewStripMock(rows: Int) {
    if (rows <= 0) return
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(rows) { i ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("✓", style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold, color = LightGreen)
                    Text("Player ${i + 1} got 7♠ from Priya",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

/** Approximates the bottom NavigationBar (tabs) + ActionButtons heights. */
@Composable
private fun PreviewBottomChromeMock() {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("[ Table | Hand ]  — nav bar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.fillMaxWidth().height(68.dp), contentAlignment = Alignment.Center) {
                Text("[  Ask   |   Claim  ]  — action buttons",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TableTabPortraitHost(stripRows: Int) {
    LiteratureTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                // Mock header (ScoreBar + turn indicator)
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(Modifier.fillMaxWidth().height(84.dp), contentAlignment = Alignment.Center) {
                        Text("Score / Turn — header",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // The real content slot — outlined to show where TableTab can clip/scroll
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    TableTab(uiState = previewUiStateWithCards)
                }
                PreviewStripMock(rows = stripRows)
                PreviewBottomChromeMock()
            }
        }
    }
}

@Preview(name = "Table · small phone · no strip", widthDp = 320, heightDp = 506, showBackground = true)
@Composable
private fun PreviewTableSmallNoStrip() = TableTabPortraitHost(stripRows = 0)

@Preview(name = "Table · small phone · 5 strips", widthDp = 320, heightDp = 506, showBackground = true)
@Composable
private fun PreviewTableSmall5Strips() = TableTabPortraitHost(stripRows = 5)

@Preview(name = "Table · large phone · no strip", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun PreviewTableLargeNoStrip() = TableTabPortraitHost(stripRows = 0)

@Preview(name = "Table · large phone · 5 strips", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun PreviewTableLarge5Strips() = TableTabPortraitHost(stripRows = 5)
