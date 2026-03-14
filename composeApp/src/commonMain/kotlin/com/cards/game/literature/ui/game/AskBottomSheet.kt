package com.cards.game.literature.ui.game

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.symbol
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.PlayerInfo

private fun suitFor(hs: HalfSuit): Suit = when (hs) {
    HalfSuit.SPADES_LOW, HalfSuit.SPADES_HIGH -> Suit.SPADES
    HalfSuit.HEARTS_LOW, HalfSuit.HEARTS_HIGH -> Suit.HEARTS
    HalfSuit.DIAMONDS_LOW, HalfSuit.DIAMONDS_HIGH -> Suit.DIAMONDS
    HalfSuit.CLUBS_LOW, HalfSuit.CLUBS_HIGH -> Suit.CLUBS
}

private fun halfSuitFor(suit: Suit, isLow: Boolean): HalfSuit = when (suit) {
    Suit.SPADES -> if (isLow) HalfSuit.SPADES_LOW else HalfSuit.SPADES_HIGH
    Suit.HEARTS -> if (isLow) HalfSuit.HEARTS_LOW else HalfSuit.HEARTS_HIGH
    Suit.DIAMONDS -> if (isLow) HalfSuit.DIAMONDS_LOW else HalfSuit.DIAMONDS_HIGH
    Suit.CLUBS -> if (isLow) HalfSuit.CLUBS_LOW else HalfSuit.CLUBS_HIGH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskBottomSheet(
    myHandByHalfSuit: Map<HalfSuit, List<Card>>,
    opponents: List<PlayerInfo>,
    initialSuit: Suit? = null,
    initialIsLow: Boolean? = null,
    onSuitSelected: (Suit?) -> Unit = {},
    onIsLowSelected: (Boolean?) -> Unit = {},
    onConfirm: (targetId: String, cards: List<Card>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSuit by remember { mutableStateOf(initialSuit) }
    var selectedIsLow by remember { mutableStateOf(initialIsLow) }
    val selectedCards = remember { mutableStateListOf<Card>() }
    var selectedOpponent by remember { mutableStateOf<PlayerInfo?>(null) }

    val availableSuits = myHandByHalfSuit.keys.map { suitFor(it) }.toSet()

    val availableHalves: Set<Boolean> = selectedSuit?.let { suit ->
        myHandByHalfSuit.keys.filter { suitFor(it) == suit }.map { it.name.endsWith("_LOW") }.toSet()
    } ?: emptySet()

    val selectedHalfSuit: HalfSuit? = if (selectedSuit != null && selectedIsLow != null)
        halfSuitFor(selectedSuit!!, selectedIsLow!!) else null

    val activeOpponents = opponents.filter { it.isActive }
    val canConfirm = selectedCards.isNotEmpty() && selectedOpponent != null

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Ask a Card",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )

            // Suit chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Suit.entries.forEach { suit ->
                    FilterChip(
                        selected = selectedSuit == suit,
                        enabled = suit in availableSuits,
                        onClick = {
                            if (selectedSuit != suit) {
                                selectedSuit = suit
                                selectedIsLow = null
                                onSuitSelected(suit)
                                onIsLowSelected(null)
                            }
                        },
                        label = { Text(suit.symbol, fontSize = 20.sp) }
                    )
                }
            }

            // Low/High chips — slides in when a suit is selected
            AnimatedVisibility(
                visible = selectedSuit != null,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true, false).forEach { isLow ->
                        if (isLow in availableHalves) {
                            FilterChip(
                                selected = selectedIsLow == isLow,
                                onClick = {
                                    if (selectedIsLow != isLow) {
                                        selectedIsLow = isLow
                                        onIsLowSelected(isLow)
                                    }
                                },
                                label = { Text(if (isLow) "Low" else "High") }
                            )
                        }
                    }
                }
            }

            // Card area — fixed height, crossfades between placeholder and grid
            AnimatedContent(
                targetState = selectedHalfSuit,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "CardArea"
            ) { halfSuit ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (halfSuit == null) {
                        Text(
                            text = if (selectedSuit == null) "Select a suit to see cards"
                                   else "Select Low or High",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val cards = remember(halfSuit) {
                            val all = DeckUtils.getAllCardsForHalfSuit(halfSuit)
                            val mine = myHandByHalfSuit[halfSuit] ?: emptyList()
                            all.filter { it !in mine }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(cards) { card ->
                                val badgeIndex = selectedCards.indexOf(card)
                                CardView(
                                    card = card,
                                    isSelected = card in selectedCards,
                                    onClick = {
                                        if (card in selectedCards) selectedCards.remove(card)
                                        else selectedCards.add(card)
                                    },
                                    modifier = Modifier.width(52.dp).height(70.dp),
                                    badgeNumber = if (badgeIndex >= 0) badgeIndex + 1 else null
                                )
                            }
                        }
                    }
                }
            }

            // Ask Queue — slides in when cards are queued
            AnimatedVisibility(
                visible = selectedCards.isNotEmpty(),
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Ask Queue:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedCards.toList().forEach { card ->
                            FilterChip(
                                selected = false,
                                onClick = { selectedCards.remove(card) },
                                label = { Text("${card.value.displayName}${card.suit.symbol} \u00d7") }
                            )
                        }
                    }
                }
            }

            // Opponent chips
            if (activeOpponents.isNotEmpty()) {
                Text("Ask:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    activeOpponents.forEach { opp ->
                        FilterChip(
                            selected = selectedOpponent == opp,
                            onClick = { selectedOpponent = opp },
                            label = { Text("${opp.name} (${opp.cardCount})") }
                        )
                    }
                }
            }

            // Cancel / Confirm buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onConfirm(selectedOpponent!!.id, selectedCards.toList()) },
                    enabled = canConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Confirm (${selectedCards.size}) \u2713")
                }
            }
        }
    }
}
