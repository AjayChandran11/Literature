package com.cards.game.literature.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.isRed
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.ui.theme.LiteratureTheme
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.cards.game.literature.ui.common.displayEmoji

/**
 * Full-screen game log — a proper page (top bar + back), not a bottom sheet. The moves render as a
 * status-badged list: asks show asker → target with the card chip; claims are highlighted rows with
 * a flag badge and the half-suit chip. Opened over the result screen; [onClose] pops it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLogScreen(events: List<GameEvent>, onClose: () -> Unit) {
    val shown = remember(events) {
        events.filter {
            it is GameEvent.CardAsked || it is GameEvent.DeckClaimed ||
                it is GameEvent.GameEnded || it is GameEvent.TurnTimedOut
        }
    }
    // Same Scaffold + TopAppBar as the Settings / Stats screens, so the header is consistent across
    // the app. It renders as an overlay inside the result screen's safe-area box, so the bar/content
    // insets are already applied by that parent — zero them here to avoid doubling top/bottom padding.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.result_log_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_back)
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shown) { event -> GameLogRow(event) }
        }
    }
}

@Composable
private fun GameLogRow(event: GameEvent) {
    when (event) {
        is GameEvent.CardAsked -> AskRow(event)
        is GameEvent.DeckClaimed -> ClaimRow(event)
        is GameEvent.TurnTimedOut -> TimeoutRow(event)
        is GameEvent.GameEnded -> EndRow()
        else -> {}
    }
}

@Composable
private fun AskRow(e: GameEvent.CardAsked) {
    val color = if (e.success) LightGreen else CardRed
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StatusBadge(if (e.success) Icons.Filled.Check else Icons.Filled.Close, color)
        Spacer(Modifier.width(12.dp))
        Text(
            text = "${e.askerName}  →  ${e.targetName}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        CardChip(e.card)
    }
}

@Composable
private fun ClaimRow(e: GameEvent.DeckClaimed) {
    // Claims are the pivotal moments — give them a tinted, rounded row and a flag badge.
    val color = if (e.correct) LightGreen else CardRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusBadge(Icons.Filled.Flag, color)
        Spacer(Modifier.width(12.dp))
        Text(
            text = e.claimerName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        HalfSuitChip(e.halfSuit)
    }
}

@Composable
private fun TimeoutRow(e: GameEvent.TurnTimedOut) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        StatusBadge(Icons.Filled.Timer, MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(Res.string.game_log_timed_out, e.playerName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EndRow() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(
            text = stringResource(Res.string.game_log_game_over),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun StatusBadge(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun CardChip(card: Card) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = card.displayEmoji,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold,
            color = if (card.suit.isRed) CardRed else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HalfSuitChip(halfSuit: HalfSuit) {
    val red = halfSuit == HalfSuit.HEARTS_LOW || halfSuit == HalfSuit.HEARTS_HIGH ||
        halfSuit == HalfSuit.DIAMONDS_LOW || halfSuit == HalfSuit.DIAMONDS_HIGH
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = halfSuit.displayName,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (red) CardRed else MaterialTheme.colorScheme.onSurface
        )
    }
}

// Open this in Android Studio's Interactive Preview (the pointer icon) to scroll the log and tap
// the back arrow — no device needed.
@Preview(name = "Game Log")
@Composable
private fun GameLogScreenPreview() {
    LiteratureTheme {
        GameLogScreen(events = previewLogEvents, onClose = {})
    }
}

private val previewLogEvents: List<GameEvent> = listOf(
    GameEvent.CardAsked(askerId = "p1", askerName = "Asha", targetId = "p2", targetName = "Ravi", card = Card(Suit.SPADES, CardValue.SEVEN), success = true),
    GameEvent.CardAsked(askerId = "p2", askerName = "Ravi", targetId = "p3", targetName = "Meera", card = Card(Suit.HEARTS, CardValue.SIX), success = false),
    GameEvent.CardAsked(askerId = "p3", askerName = "Meera", targetId = "p1", targetName = "Asha", card = Card(Suit.DIAMONDS, CardValue.KING), success = true),
    GameEvent.DeckClaimed(claimerId = "p1", claimerName = "Asha", teamId = "team_1", halfSuit = HalfSuit.SPADES_LOW, correct = true),
    GameEvent.CardAsked(askerId = "p4", askerName = "Bruno", targetId = "p2", targetName = "Ravi", card = Card(Suit.CLUBS, CardValue.NINE), success = false),
    GameEvent.CardAsked(askerId = "p1", askerName = "Asha", targetId = "p4", targetName = "Bruno", card = Card(Suit.HEARTS, CardValue.QUEEN), success = true),
    GameEvent.TurnTimedOut(playerId = "p2", playerName = "Ravi"),
    GameEvent.DeckClaimed(claimerId = "p3", claimerName = "Meera", teamId = "team_2", halfSuit = HalfSuit.HEARTS_HIGH, correct = false),
    GameEvent.CardAsked(askerId = "p2", askerName = "Ravi", targetId = "p1", targetName = "Asha", card = Card(Suit.DIAMONDS, CardValue.TWO), success = true),
    GameEvent.CardAsked(askerId = "p3", askerName = "Meera", targetId = "p4", targetName = "Bruno", card = Card(Suit.SPADES, CardValue.JACK), success = true),
    GameEvent.DeckClaimed(claimerId = "p1", claimerName = "Asha", teamId = "team_1", halfSuit = HalfSuit.DIAMONDS_LOW, correct = true),
    GameEvent.CardAsked(askerId = "p4", askerName = "Bruno", targetId = "p3", targetName = "Meera", card = Card(Suit.CLUBS, CardValue.ACE), success = false),
    GameEvent.DeckClaimed(claimerId = "p2", claimerName = "Ravi", teamId = "team_1", halfSuit = HalfSuit.CLUBS_HIGH, correct = true),
    GameEvent.GameEnded(winnerTeamId = "team_1"),
)
