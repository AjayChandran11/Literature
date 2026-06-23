package com.cards.game.literature.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

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
                stringResource(Res.string.game_log_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                if (expanded) stringResource(Res.string.game_log_collapse) else stringResource(Res.string.game_log_expand),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (recentEvents.isEmpty()) {
            Text(
                stringResource(Res.string.game_log_game_started),
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
    val color: Color
    val icon: ImageVector?
    val text: String
    val highlight: String?   // card or half-suit name, emphasized in the sentence
    when (event) {
        is GameEvent.CardAsked -> {
            color = if (event.success) LightGreen else CardRed
            icon = if (event.success) Icons.Filled.Check else Icons.Filled.Close
            highlight = event.card.displayName
            text = if (event.success)
                stringResource(Res.string.game_log_got_card, event.askerName, event.card.displayName, event.targetName)
            else
                stringResource(Res.string.game_log_asked_no, event.askerName, event.targetName, event.card.displayName)
        }
        is GameEvent.DeckClaimed -> {
            color = if (event.correct) LightGreen else CardRed
            icon = if (event.correct) Icons.Filled.Check else Icons.Filled.Close
            highlight = event.halfSuit.displayName
            text = if (event.correct)
                stringResource(Res.string.game_log_claimed_correctly, event.claimerName, event.halfSuit.displayName)
            else
                stringResource(Res.string.game_log_claimed_incorrectly, event.claimerName, event.halfSuit.displayName)
        }
        is GameEvent.GameEnded -> {
            color = MaterialTheme.colorScheme.secondary; icon = null; highlight = null
            text = stringResource(Res.string.game_log_game_over)
        }
        else -> return
    }
    val annotated = if (highlight == null) AnnotatedString(text) else {
        val idx = text.indexOf(highlight)
        if (idx < 0) AnnotatedString(text) else buildAnnotatedString {
            append(text.substring(0, idx))
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(highlight) }
            append(text.substring(idx + highlight.length))
        }
    }
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text = annotated, fontSize = fontSize, color = color)
    }
}
