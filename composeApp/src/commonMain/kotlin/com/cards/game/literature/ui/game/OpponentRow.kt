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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.bot.BotPersonalities
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.viewmodel.PlayerInfo
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun OpponentRow(opponents: List<PlayerInfo>, modifier: Modifier = Modifier) {
    AvatarRow(players = opponents, isOpponent = true, modifier = modifier)
}

@Composable
fun TeammateRow(teammates: List<PlayerInfo>, modifier: Modifier = Modifier) {
    AvatarRow(players = teammates, isOpponent = false, modifier = modifier)
}

/**
 * Row of player avatars that adapts to the space it actually gets. Each avatar
 * naturally wants 88dp; when the row is narrower than count x 88dp (e.g. four
 * opponents in the side-by-side left pane on a foldable), every avatar shrinks
 * to an equal share instead of the last one getting crushed off the edge.
 * When space allows, the 88dp width is unchanged from before.
 */
@Composable
private fun AvatarRow(players: List<PlayerInfo>, isOpponent: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val avatarWidth = (maxWidth / players.size.coerceAtLeast(1)).coerceAtMost(88.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            players.forEach { player ->
                PlayerAvatar(player = player, isOpponent = isOpponent, width = avatarWidth)
            }
        }
    }
}

/**
 * One-shot scale pop the moment the turn lands on this player — a distinct
 * "it's their move now" beat on top of the ongoing border pulse. Springs from
 * a slight dip so the overshoot reads as a bounce, then settles at rest.
 */
@Composable
private fun turnPopScale(isCurrentTurn: Boolean): Float {
    val pop = remember { Animatable(1f) }
    LaunchedEffect(isCurrentTurn) {
        if (isCurrentTurn) {
            pop.snapTo(0.85f)
            pop.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
    return pop.value
}

@Composable
fun PlayerAvatar(player: PlayerInfo, isOpponent: Boolean, width: Dp = 88.dp) {
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

    // Circle keeps its 64dp look while the cell has room, shrinking only when
    // the row is genuinely tighter than that (see AvatarRow).
    val circleSize = (width - 4.dp).coerceAtMost(64.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(width).semantics { contentDescription = avatarDesc }
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .scale(turnPopScale(player.isCurrentTurn))
                .clip(CircleShape)
                .background(
                    if (isOpponent) CardRed.copy(alpha = 0.3f * alpha)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f * alpha)
                )
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (player.isBot) {
                Text(
                    text = BotPersonalities.emojiFor(player.name),
                    fontSize = 30.sp,
                    modifier = Modifier.alpha(alpha)
                )
            } else {
                Text(
                    text = player.name.first().uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = player.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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

// ─── Compact variants for landscape ─────────────────────────────────────────

@Composable
fun CompactOpponentRow(opponents: List<PlayerInfo>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        opponents.forEach { player ->
            CompactPlayerAvatar(player = player, isOpponent = true)
        }
    }
}

@Composable
fun CompactTeammateRow(teammates: List<PlayerInfo>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        teammates.forEach { player ->
            CompactPlayerAvatar(player = player, isOpponent = false)
        }
    }
}

@Composable
fun CompactPlayerAvatar(player: PlayerInfo, isOpponent: Boolean) {
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = avatarDesc },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .scale(turnPopScale(player.isCurrentTurn))
                .clip(CircleShape)
                .background(
                    if (isOpponent) CardRed.copy(alpha = 0.3f * alpha)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f * alpha)
                )
                .border(1.5.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (player.isBot) {
                Text(
                    text = BotPersonalities.emojiFor(player.name),
                    fontSize = 16.sp,
                    modifier = Modifier.alpha(alpha)
                )
            } else {
                Text(
                    text = player.name.first().uppercase(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = player.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (player.cardCount == 0) {
            Text(
                text = stringResource(Res.string.player_out_of_cards),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = alpha)
            )
        } else {
            Text(
                text = stringResource(Res.string.player_card_count, player.cardCount),
                style = MaterialTheme.typography.labelSmall,
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

@Preview(name = "Avatar — bots with personalities")
@Composable
private fun PreviewBotAvatars() {
    LiteratureTheme {
        Surface {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
                PlayerAvatar(
                    player = PlayerInfo("b1", "Alice", cardCount = 6, isActive = true, isCurrentTurn = true, isBot = true),
                    isOpponent = true
                )
                PlayerAvatar(
                    player = PlayerInfo("b2", "Bob", cardCount = 3, isActive = true, isCurrentTurn = false, isBot = true),
                    isOpponent = false
                )
                // Name not in the roster (e.g. a replaced human) → generic 🤖 fallback
                PlayerAvatar(
                    player = PlayerInfo("b3", "Ravi", cardCount = 0, isActive = false, isCurrentTurn = false, isBot = true),
                    isOpponent = true
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
