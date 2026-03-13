package com.cards.game.literature.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Your Cards",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = GoldAccent,
            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Start
        ) {
            handByHalfSuit.entries
                .sortedBy { it.key.ordinal }
                .forEach { (halfSuit, cards) ->
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            halfSuit.displayName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                            cards.forEach { card ->
                                CardView(
                                    card = card,
                                    isSelected = card == selectedCard,
                                    onClick = { onCardSelected(card) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
        }
    }
}

@Composable
fun CardView(card: Card, isSelected: Boolean, onClick: () -> Unit) {
    val cardColor = if (card.suit.isRed) CardRed else Color.White
    val bgColor = if (isSelected) GoldAccent.copy(alpha = 0.3f) else Color(0xFF1A1A2E)
    val borderColor = if (isSelected) GoldAccent else Color(0xFF4A4A6A)

    Box(
        modifier = Modifier
            .width(72.dp)
            .height(100.dp)
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
    }
}
