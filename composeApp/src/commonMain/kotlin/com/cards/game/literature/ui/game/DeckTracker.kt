package com.cards.game.literature.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.isRed
import com.cards.game.literature.model.symbol
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private val HalfSuit.suit: Suit
    get() = when (this) {
        HalfSuit.SPADES_LOW, HalfSuit.SPADES_HIGH -> Suit.SPADES
        HalfSuit.HEARTS_LOW, HalfSuit.HEARTS_HIGH -> Suit.HEARTS
        HalfSuit.DIAMONDS_LOW, HalfSuit.DIAMONDS_HIGH -> Suit.DIAMONDS
        HalfSuit.CLUBS_LOW, HalfSuit.CLUBS_HIGH -> Suit.CLUBS
    }

private val HalfSuit.isLow: Boolean get() = name.endsWith("_LOW")

/** Compact DeckTracker for landscape: smaller items. */
@Composable
fun CompactDeckTracker(
    statuses: List<HalfSuitStatus>,
    myTeamId: String,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(statuses) { status ->
            val bgColor = when {
                status.claimedByTeamId == null -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                status.claimedByTeamId == myTeamId -> LightGreen.copy(alpha = 0.3f)
                else -> CardRed.copy(alpha = 0.3f)
            }
            val borderColor = when {
                status.claimedByTeamId == null -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                status.claimedByTeamId == myTeamId -> LightGreen
                else -> CardRed
            }

            val deckDesc = when {
                status.claimedByTeamId == null -> stringResource(Res.string.cd_deck_open, status.halfSuit.displayName)
                status.claimedByTeamId == myTeamId -> stringResource(Res.string.cd_deck_ours, status.halfSuit.displayName)
                else -> stringResource(Res.string.cd_deck_theirs, status.halfSuit.displayName)
            }
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .background(bgColor, RoundedCornerShape(6.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .padding(6.dp)
                    .semantics { contentDescription = deckDesc },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = status.halfSuit.displayName.replace(" ", "\n"),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                )
                if (status.claimedByTeamId != null) {
                    val label = if (status.claimedByTeamId == myTeamId)
                        stringResource(Res.string.deck_tracker_ours)
                    else
                        stringResource(Res.string.deck_tracker_theirs)
                    val labelColor = if (status.claimedByTeamId == myTeamId) LightGreen else CardRed
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.deck_tracker_open),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun DeckTracker(
    statuses: List<HalfSuitStatus>,
    myTeamId: String,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(statuses) { status ->
            val bgColor = when {
                status.claimedByTeamId == null -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                status.claimedByTeamId == myTeamId -> LightGreen.copy(alpha = 0.3f)
                else -> CardRed.copy(alpha = 0.3f)
            }
            val borderColor = when {
                status.claimedByTeamId == null -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                status.claimedByTeamId == myTeamId -> LightGreen
                else -> CardRed
            }

            val deckDesc = when {
                status.claimedByTeamId == null -> stringResource(Res.string.cd_deck_open, status.halfSuit.displayName)
                status.claimedByTeamId == myTeamId -> stringResource(Res.string.cd_deck_ours, status.halfSuit.displayName)
                else -> stringResource(Res.string.cd_deck_theirs, status.halfSuit.displayName)
            }
            Column(
                modifier = Modifier
                    .width(96.dp)
                    .background(bgColor, RoundedCornerShape(6.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .padding(10.dp)
                    .semantics { contentDescription = deckDesc },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = status.halfSuit.displayName.replace(" ", "\n"),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                )
                if (status.claimedByTeamId != null) {
                    val label = if (status.claimedByTeamId == myTeamId)
                        stringResource(Res.string.deck_tracker_ours)
                    else
                        stringResource(Res.string.deck_tracker_theirs)
                    val labelColor = if (status.claimedByTeamId == myTeamId) LightGreen else CardRed
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.deck_tracker_open),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Grid-based DeckTracker for side-by-side (tablet) mode.
 * Shows 8 half-suits in a 2-column grid (4 rows), filling vertical space
 * much better than the horizontal LazyRow used on phones.
 */
@Composable
fun SideBySideDeckTracker(
    statuses: List<HalfSuitStatus>,
    myTeamId: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        statuses.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { status ->
                    SideBySideDeckCell(
                        status = status,
                        myTeamId = myTeamId,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SideBySideDeckCell(
    status: HalfSuitStatus,
    myTeamId: String,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        status.claimedByTeamId == null -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        status.claimedByTeamId == myTeamId -> LightGreen.copy(alpha = 0.15f)
        else -> CardRed.copy(alpha = 0.15f)
    }
    val borderColor = when {
        status.claimedByTeamId == null -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        status.claimedByTeamId == myTeamId -> LightGreen
        else -> CardRed
    }
    val suit = status.halfSuit.suit
    val suitColor = if (suit.isRed) CardRed else MaterialTheme.colorScheme.onSurface

    val deckDesc = when {
        status.claimedByTeamId == null -> stringResource(Res.string.cd_deck_open, status.halfSuit.displayName)
        status.claimedByTeamId == myTeamId -> stringResource(Res.string.cd_deck_ours, status.halfSuit.displayName)
        else -> stringResource(Res.string.cd_deck_theirs, status.halfSuit.displayName)
    }

    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .semantics { contentDescription = deckDesc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = suit.symbol,
            fontSize = 22.sp,
            color = suitColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (status.halfSuit.isLow)
                stringResource(Res.string.ask_filter_low)
            else
                stringResource(Res.string.ask_filter_high),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        if (status.claimedByTeamId != null) {
            val label = if (status.claimedByTeamId == myTeamId)
                stringResource(Res.string.deck_tracker_ours)
            else
                stringResource(Res.string.deck_tracker_theirs)
            val labelColor = if (status.claimedByTeamId == myTeamId) LightGreen else CardRed
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = labelColor.copy(alpha = 0.2f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        } else {
            Text(
                text = stringResource(Res.string.deck_tracker_open),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
