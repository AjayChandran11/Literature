package com.cards.game.literature.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

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
