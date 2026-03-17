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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.GameUiState

enum class GameTab(val label: String, val icon: ImageVector) {
    TABLE("Table", Icons.Filled.GridView),
    HAND("Hand", Icons.Filled.PanTool),
}

// enum class LogFilter { ALL, ASKS, CLAIMS }  // Only used by LogTab

@Composable
fun TableTab(uiState: GameUiState) {
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
            SectionLabel("OPPONENTS")
            OpponentRow(opponents = uiState.opponents)
        }

        // Teammates section — takes equal space
        if (uiState.teammates.isNotEmpty()) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                SectionLabel("TEAMMATES")
                TeammateRow(teammates = uiState.teammates)
            }
        }

        // Half-suits pinned at the bottom
        SectionLabel("HALF-SUITS")
        DeckTracker(
            statuses = uiState.halfSuitStatuses,
            myTeamId = uiState.myTeamId
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun HandTab(uiState: GameUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            "YOUR HAND",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = GoldAccent,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CardHand(
            handByHalfSuit = uiState.myHandByHalfSuit,
            modifier = Modifier.weight(1f)
        )
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
    )
}
