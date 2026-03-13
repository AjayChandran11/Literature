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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.ui.theme.LightGreen

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
                status.claimedByTeamId == null -> GoldAccent.copy(alpha = 0.2f)
                status.claimedByTeamId == myTeamId -> LightGreen.copy(alpha = 0.3f)
                else -> CardRed.copy(alpha = 0.3f)
            }
            val borderColor = when {
                status.claimedByTeamId == null -> GoldAccent.copy(alpha = 0.5f)
                status.claimedByTeamId == myTeamId -> LightGreen
                else -> CardRed
            }

            Column(
                modifier = Modifier
                    .width(96.dp)
                    .background(bgColor, RoundedCornerShape(6.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = status.halfSuit.displayName.replace(" ", "\n"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    minLines = 2,
                    maxLines = 2,
                )
                if (status.claimedByTeamId != null) {
                    val label = if (status.claimedByTeamId == myTeamId) "Ours" else "Theirs"
                    val labelColor = if (status.claimedByTeamId == myTeamId) LightGreen else CardRed
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = labelColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Open",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
