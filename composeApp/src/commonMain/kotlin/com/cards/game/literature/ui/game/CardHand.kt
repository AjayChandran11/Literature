package com.cards.game.literature.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.ui.tooling.preview.Preview
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.isRed
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LiteratureTheme
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.cd_card
import org.jetbrains.compose.resources.stringResource

@Composable
fun CardHand(
    handByHalfSuit: Map<HalfSuit, List<Card>>,
    modifier: Modifier = Modifier
) {
    val sortedEntries = handByHalfSuit.entries
        .sortedBy { it.key.ordinal }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = sortedEntries.toList(),
            key = { it.key.ordinal }
        ) { (halfSuit, cards) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(
                        fadeInSpec = tween(300),
                        fadeOutSpec = tween(300),
                        placementSpec = spring<IntOffset>(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    halfSuit.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(72.dp).padding(end = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = cards,
                        key = { "${it.suit}_${it.value}" }
                    ) { card ->
                        AnimatedCardView(
                            card = card,
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(250),
                                fadeOutSpec = tween(250),
                                placementSpec = spring<IntOffset>(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedCardView(card: Card, modifier: Modifier = Modifier) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = EaseOutCubic)
    )
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    val translationY by animateFloatAsState(
        targetValue = if (appeared) 0f else 24f,
        animationSpec = tween(durationMillis = 350, easing = EaseOutCubic)
    )

    CardView(
        card = card,
        isSelected = false,
        onClick = { },
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                this.translationY = translationY
            }
    )
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
    val bgColor = if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f) else Color.White
    val borderColor = if (isSelected) MaterialTheme.colorScheme.secondary else Color.LightGray

    val cardDesc = stringResource(Res.string.cd_card, card.value.displayName, card.suit.name.lowercase().replaceFirstChar { it.uppercase() })
    Box(
        modifier = modifier
            .width(60.dp)
            .height(80.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = cardDesc }
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
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
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

// ─── Interactive Animation Preview ──────────────────────────────────────────

@Preview(name = "CardHand — animation test", showBackground = true)
@Composable
private fun PreviewCardHandAnimation() {
    val allCards = remember {
        listOf(
            Card(Suit.SPADES, CardValue.TWO),
            Card(Suit.SPADES, CardValue.THREE),
            Card(Suit.SPADES, CardValue.FOUR),
            Card(Suit.SPADES, CardValue.FIVE),
            Card(Suit.SPADES, CardValue.SIX),
            Card(Suit.HEARTS, CardValue.TWO),
            Card(Suit.HEARTS, CardValue.THREE),
            Card(Suit.HEARTS, CardValue.FOUR),
            Card(Suit.DIAMONDS, CardValue.NINE),
            Card(Suit.DIAMONDS, CardValue.TEN),
            Card(Suit.DIAMONDS, CardValue.JACK),
            Card(Suit.CLUBS, CardValue.ACE),
            Card(Suit.CLUBS, CardValue.KING),
        )
    }

    var hand by remember { mutableStateOf(allCards.take(8)) }
    val pool = allCards - hand.toSet()

    fun groupByHalfSuit(cards: List<Card>): Map<HalfSuit, List<Card>> =
        cards.groupBy { card ->
            val isLow = card.value.rank in 1..7
            when (card.suit) {
                Suit.SPADES -> if (isLow) HalfSuit.SPADES_LOW else HalfSuit.SPADES_HIGH
                Suit.HEARTS -> if (isLow) HalfSuit.HEARTS_LOW else HalfSuit.HEARTS_HIGH
                Suit.DIAMONDS -> if (isLow) HalfSuit.DIAMONDS_LOW else HalfSuit.DIAMONDS_HIGH
                Suit.CLUBS -> if (isLow) HalfSuit.CLUBS_LOW else HalfSuit.CLUBS_HIGH
            }
        }

    LiteratureTheme {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                "Cards in hand: ${hand.size} | Pool: ${pool.size}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (hand.isNotEmpty()) {
                            hand = hand.dropLast(1)
                        }
                    },
                    enabled = hand.isNotEmpty()
                ) { Text("Remove last") }

                Button(
                    onClick = {
                        if (pool.isNotEmpty()) {
                            hand = hand + pool.first()
                        }
                    },
                    enabled = pool.isNotEmpty()
                ) { Text("Add card") }

                Button(
                    onClick = {
                        if (hand.size >= 2) {
                            val idx = (0 until hand.size).random()
                            hand = hand.toMutableList().apply { removeAt(idx) }
                        }
                    },
                    enabled = hand.size >= 2
                ) { Text("Remove random") }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CardHand(
                handByHalfSuit = groupByHalfSuit(hand),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
