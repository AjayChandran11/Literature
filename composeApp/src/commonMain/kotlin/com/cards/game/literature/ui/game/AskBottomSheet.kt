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
import androidx.compose.runtime.saveable.rememberSaveable
import com.cards.game.literature.ui.common.cardListSaver
import com.cards.game.literature.ui.common.nullableEnumSaver
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import com.cards.game.literature.ui.common.WindowSize.isCompactHeight
import com.cards.game.literature.ui.common.WindowSize.useSideBySide
import com.cards.game.literature.viewmodel.PlayerInfo
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.cards.game.literature.ui.common.emoji
import com.cards.game.literature.ui.common.displayEmoji
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** A suit chip's label is a bare glyph, which TalkBack reads as nothing useful (or as
 *  "black spade suit"). This names it, and it is localised — the old version was a dead
 *  hard-coded English helper nothing called. */
@Composable
private fun Suit.accessibleName(): String = stringResource(
    when (this) {
        Suit.SPADES -> Res.string.cd_suit_spades
        Suit.HEARTS -> Res.string.cd_suit_hearts
        Suit.DIAMONDS -> Res.string.cd_suit_diamonds
        Suit.CLUBS -> Res.string.cd_suit_clubs
    }
)

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
    // Saved, not just remembered: a rotation or a system dark-mode switch recreates the Activity,
    // and a plain remember threw away a queue of cards picked one by one — mid-turn, on the clock.
    var selectedSuit by rememberSaveable(stateSaver = nullableEnumSaver<Suit>()) { mutableStateOf(initialSuit) }
    var selectedIsLow by rememberSaveable { mutableStateOf(initialIsLow) }
    val selectedCards = rememberSaveable(saver = cardListSaver) { mutableStateListOf<Card>() }
    // Held by id, not by value: the opponent's own card count changes as the turn goes on, so a
    // saved copy would go stale. Resolving each time also drops the selection if they leave.
    var selectedOpponentId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedOpponent = opponents.firstOrNull { it.id == selectedOpponentId }

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
        val windowInfo = currentWindowAdaptiveInfo()
        val isCompact = windowInfo.isCompactHeight
        val gridColumns = if (windowInfo.useSideBySide) GridCells.Adaptive(56.dp) else GridCells.Fixed(3)

        @Composable
        fun CardArea(modifier: Modifier) {
            AnimatedContent(
                targetState = selectedHalfSuit,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
                label = "CardArea",
                modifier = modifier
            ) { halfSuit ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (halfSuit == null) {
                        Text(
                            text = if (selectedSuit == null) stringResource(Res.string.ask_select_suit_hint)
                                   else stringResource(Res.string.ask_select_range_hint),
                            style = MaterialTheme.typography.bodyLarge,
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
                            columns = gridColumns,
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
        }

        if (isCompact) {
            // Landscape: controls on the left, card grid on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.ask_sheet_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Suit.entries.forEach { suit ->
                            val suitName = suit.accessibleName()
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
                                label = { Text(suit.emoji, style = MaterialTheme.typography.titleMedium) },
                                modifier = Modifier.semantics { contentDescription = suitName }
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = selectedSuit != null,
                        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        label = {
                                            Text(
                                                if (isLow) stringResource(Res.string.ask_filter_low)
                                                else stringResource(Res.string.ask_filter_high)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = selectedCards.isNotEmpty(),
                        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                stringResource(Res.string.ask_queue_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                selectedCards.toList().forEach { card ->
                                    FilterChip(
                                        selected = false,
                                        onClick = { selectedCards.remove(card) },
                                        label = { Text("${card.displayEmoji} \u00d7") }
                                    )
                                }
                            }
                        }
                    }
                    if (activeOpponents.isNotEmpty()) {
                        Text(
                            stringResource(Res.string.ask_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        ) {
                            activeOpponents.forEach { opp ->
                                FilterChip(
                                    selected = selectedOpponentId == opp.id,
                                    onClick = { selectedOpponentId = opp.id },
                                    label = { Text("${opp.name} (${opp.cardCount})") }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.button_cancel))
                        }
                        Button(
                            onClick = { onConfirm(selectedOpponent!!.id, selectedCards.toList()) },
                            enabled = canConfirm,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.ask_confirm_button, selectedCards.size))
                        }
                    }
                }
                CardArea(modifier = Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.ask_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Suit.entries.forEach { suit ->
                        val suitName = suit.accessibleName()
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
                            label = { Text(suit.emoji, style = MaterialTheme.typography.headlineSmall) },
                            modifier = Modifier.semantics { contentDescription = suitName }
                        )
                    }
                }
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
                                    label = {
                                        Text(
                                            if (isLow) stringResource(Res.string.ask_filter_low)
                                            else stringResource(Res.string.ask_filter_high)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                CardArea(modifier = Modifier.fillMaxWidth().height(180.dp))
                AnimatedVisibility(
                    visible = selectedCards.isNotEmpty(),
                    enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(Res.string.ask_queue_label),
                            style = MaterialTheme.typography.bodyMedium,
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
                                    label = { Text("${card.displayEmoji} \u00d7") }
                                )
                            }
                        }
                    }
                }
                if (activeOpponents.isNotEmpty()) {
                    Text(stringResource(Res.string.ask_label), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        activeOpponents.forEach { opp ->
                            FilterChip(
                                selected = selectedOpponentId == opp.id,
                                onClick = { selectedOpponentId = opp.id },
                                label = { Text("${opp.name} (${opp.cardCount})") }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.button_cancel))
                    }
                    Button(
                        onClick = { onConfirm(selectedOpponent!!.id, selectedCards.toList()) },
                        enabled = canConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(Res.string.ask_confirm_button, selectedCards.size))
                    }
                }
            }
        }
    }
}
