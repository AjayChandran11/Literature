package com.cards.game.literature.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** Compact action buttons for landscape: shorter height, tighter padding. */
@Composable
fun CompactActionButtons(
    isMyTurn: Boolean,
    onAskCard: () -> Unit,
    onClaimDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onAskCard,
            enabled = isMyTurn,
            modifier = Modifier.weight(1f).height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Text(stringResource(Res.string.action_ask_card), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onClaimDeck,
            enabled = isMyTurn,
            modifier = Modifier.weight(1f).height(40.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Text(stringResource(Res.string.action_claim_deck), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ActionButtons(
    isMyTurn: Boolean,
    onAskCard: () -> Unit,
    onClaimDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onAskCard,
            enabled = isMyTurn,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Text(stringResource(Res.string.action_ask_card), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = onClaimDeck,
            enabled = isMyTurn,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Text(stringResource(Res.string.action_claim_deck), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
