package com.cards.game.literature.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.isRed
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent

@Composable
fun CardHand(
    handByHalfSuit: Map<HalfSuit, List<Card>>,
    selectedCard: Card?,
    onCardSelected: (Card) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        handByHalfSuit.entries
            .sortedBy { it.key.ordinal }
            .forEach { (halfSuit, cards) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        halfSuit.displayName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(72.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cards.forEach { card ->
                            CardView(
                                card = card,
                                isSelected = card == selectedCard,
                                onClick = { onCardSelected(card) }
                            )
                        }
                    }
                }
            }
    }
}

@Composable
fun CardView(
    card: Card,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeNumber: Int? = null
) {
    val cardColor = if (card.suit.isRed) CardRed else Color.Black
    val bgColor = if (isSelected) GoldAccent.copy(alpha = 0.3f) else Color.White
    val borderColor = if (isSelected) GoldAccent else Color.LightGray

    Box(
        modifier = modifier
            .width(60.dp)
            .height(80.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = card.value.displayName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = cardColor
            )
            Text(
                text = card.suit.let { suit ->
                    when (suit) {
                        Suit.SPADES -> "\u2660"
                        Suit.HEARTS -> "\u2665"
                        Suit.DIAMONDS -> "\u2666"
                        Suit.CLUBS -> "\u2663"
                    }
                },
                fontSize = 20.sp,
                color = cardColor
            )
        }
        if (badgeNumber != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(GoldAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$badgeNumber",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
