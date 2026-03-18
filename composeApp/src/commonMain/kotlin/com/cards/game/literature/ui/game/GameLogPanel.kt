package com.cards.game.literature.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen

@Composable
fun GameLogPanel(events: List<GameEvent>, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val displayEvents = events.filterNot { it is GameEvent.TurnChanged || it is GameEvent.GameStarted }
    val recentEvents = if (expanded) displayEvents else displayEvents.takeLast(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Game Log",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                if (expanded) "Collapse" else "Expand",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (recentEvents.isEmpty()) {
            Text(
                "Game started!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val maxHeight = if (expanded) 200.dp else 80.dp
            LazyColumn(
                modifier = Modifier.heightIn(max = maxHeight),
                reverseLayout = true
            ) {
                items(recentEvents.reversed()) { event ->
                    GameLogEntry(event)
                }
            }
        }
    }
}

@Composable
fun GameLogEntry(event: GameEvent, fontSize: TextUnit = 14.sp) {
    val (text, color) = when (event) {
        is GameEvent.CardAsked -> {
            if (event.success) {
                "${event.askerName} got ${event.card.displayName} from ${event.targetName}" to LightGreen
            } else {
                "${event.askerName} asked ${event.targetName} for ${event.card.displayName} - No!" to CardRed
            }
        }
        is GameEvent.DeckClaimed -> {
            if (event.correct) {
                "${event.claimerName} claimed ${event.halfSuit.displayName} correctly!" to LightGreen
            } else {
                "${event.claimerName} claimed ${event.halfSuit.displayName} incorrectly!" to CardRed
            }
        }
        is GameEvent.GameEnded -> {
            "Game Over!" to MaterialTheme.colorScheme.secondary
        }
        else -> "" to Color.Gray
    }
    if (text.isNotEmpty()) {
        Text(
            text = text,
            fontSize = fontSize,
            color = color,
            modifier = Modifier.padding(vertical = 1.dp)
        )
    }
}
