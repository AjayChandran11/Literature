package com.cards.game.literature.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.GameUiState

enum class GameTab(val label: String, val symbol: String) {
    TABLE("Table", "\u25A6"),
    HAND("Hand", "\u2660"),
    LOG("Log", "\u2630")
}

enum class LogFilter { ALL, ASKS, CLAIMS }

@Composable
fun TableTab(uiState: GameUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionLabel("OPPONENTS")
        OpponentRow(opponents = uiState.opponents)

        if (uiState.teammates.isNotEmpty()) {
            SectionLabel("TEAMMATES")
            TeammateRow(teammates = uiState.teammates)
        }

        SectionLabel("HALF-SUITS")
        DeckTracker(
            statuses = uiState.halfSuitStatuses,
            myTeamId = uiState.myTeamId
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun HandTab(
    uiState: GameUiState,
    selectedCard: Card?,
    onCardSelected: (Card) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            "YOUR HAND",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CardHand(
            handByHalfSuit = uiState.myHandByHalfSuit,
            selectedCard = selectedCard,
            onCardSelected = onCardSelected,
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(visible = selectedCard != null) {
            selectedCard?.let { card ->
                Text(
                    text = "Selected: ${card.displayName}",
                    fontSize = 13.sp,
                    color = GoldAccent,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

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
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(LogFilter.entries) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }
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

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}
