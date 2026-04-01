package com.cards.game.literature.ui.game

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.viewmodel.PlayerInfo
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun OpponentRow(opponents: List<PlayerInfo>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        opponents.forEach { player ->
            PlayerAvatar(player = player, isOpponent = true)
        }
    }
}

@Composable
fun TeammateRow(teammates: List<PlayerInfo>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        teammates.forEach { player ->
            PlayerAvatar(player = player, isOpponent = false)
        }
    }
}

@Composable
fun PlayerAvatar(player: PlayerInfo, isOpponent: Boolean) {
    val borderColor by animateColorAsState(
        targetValue = if (player.isCurrentTurn) MaterialTheme.colorScheme.secondary else Color.Transparent,
        animationSpec = if (player.isCurrentTurn) {
            infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300)
        }
    )

    val alpha = if (player.isActive) 1f else 0.4f

    val avatarDesc = when {
        player.isCurrentTurn -> stringResource(Res.string.cd_player_active, player.name, player.cardCount)
        player.cardCount == 0 -> stringResource(Res.string.cd_player_out, player.name)
        else -> stringResource(Res.string.cd_player, player.name, player.cardCount)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp).semantics { contentDescription = avatarDesc }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isOpponent) CardRed.copy(alpha = 0.3f * alpha)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f * alpha)
                )
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = player.name.first().uppercase(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = player.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        if (player.cardCount == 0) {
            Text(
                text = stringResource(Res.string.player_out_of_cards),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = alpha),
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = stringResource(Res.string.player_card_count, player.cardCount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(name = "Avatar — with cards, active")
@Composable
private fun PreviewPlayerAvatarWithCards() {
    LiteratureTheme {
        Surface {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
                PlayerAvatar(
                    player = PlayerInfo("1", "Rahul", cardCount = 8, isActive = true, isCurrentTurn = false),
                    isOpponent = true
                )
                PlayerAvatar(
                    player = PlayerInfo("2", "Priya", cardCount = 5, isActive = true, isCurrentTurn = true),
                    isOpponent = false
                )
            }
        }
    }
}

@Preview(name = "Avatar — out of cards")
@Composable
private fun PreviewPlayerAvatarOutOfCards() {
    LiteratureTheme {
        Surface {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(16.dp)) {
                PlayerAvatar(
                    player = PlayerInfo("3", "Amit", cardCount = 0, isActive = false, isCurrentTurn = false),
                    isOpponent = true
                )
                PlayerAvatar(
                    player = PlayerInfo("4", "Sneha", cardCount = 0, isActive = false, isCurrentTurn = false),
                    isOpponent = false
                )
            }
        }
    }
}

@Preview(name = "OpponentRow — mixed")
@Composable
private fun PreviewOpponentRow() {
    LiteratureTheme {
        Surface {
            OpponentRow(
                opponents = listOf(
                    PlayerInfo("1", "Rahul", cardCount = 8, isActive = true, isCurrentTurn = true),
                    PlayerInfo("2", "Priya", cardCount = 0, isActive = false, isCurrentTurn = false),
                    PlayerInfo("3", "Amit", cardCount = 4, isActive = true, isCurrentTurn = false),
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
