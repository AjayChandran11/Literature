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
import androidx.compose.ui.unit.sp
import com.cards.game.literature.ui.theme.GoldAccent

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
            Text("ASK CARD", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Button(
            onClick = onClaimDeck,
            enabled = isMyTurn,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        ) {
            Text("CLAIM DECK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
