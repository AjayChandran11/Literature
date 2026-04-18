package com.cards.game.literature.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.*
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.viewmodel.PlayerInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import com.cards.game.literature.ui.common.WindowSize.isCompactHeight
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

enum class ClaimStep { SELECT_HALF_SUIT, ASSIGN_CARDS, CONFIRM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimBottomSheet(
    myPlayerId: String,
    myHand: List<Card>,
    myHalfSuits: Set<HalfSuit>,
    teammates: List<PlayerInfo>,
    halfSuitStatuses: List<HalfSuitStatus>,
    onConfirm: (ClaimDeclaration) -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(ClaimStep.SELECT_HALF_SUIT) }
    var selectedHalfSuit by remember { mutableStateOf<HalfSuit?>(null) }
    var assignments by remember { mutableStateOf<MutableMap<Card, String>>(mutableMapOf()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val claimYouLabel = stringResource(Res.string.claim_you)
    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.isCompactHeight

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (isCompact) 16.dp else 20.dp,
                    vertical = if (isCompact) 8.dp else 16.dp
                )
        ) {
            Text(
                text = when (step) {
                    ClaimStep.SELECT_HALF_SUIT -> stringResource(Res.string.claim_step1_title)
                    ClaimStep.ASSIGN_CARDS -> stringResource(Res.string.claim_step2_title)
                    ClaimStep.CONFIRM -> stringResource(Res.string.claim_confirm_title)
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 16.dp))

            when (step) {
                ClaimStep.SELECT_HALF_SUIT -> {
                    // Show only unclaimed half-suits that the player has cards in
                    val availableHalfSuits = myHalfSuits.filter { hs ->
                        halfSuitStatuses.find { it.halfSuit == hs }?.claimedByTeamId == null
                    }
                    availableHalfSuits.sortedBy { it.ordinal }.forEach { hs ->
                        ListItem(
                            headlineContent = { Text(hs.displayName,
                                style = MaterialTheme.typography.titleMedium) },
                            modifier = Modifier.clickable {
                                selectedHalfSuit = hs
                                // Auto-assign my own cards
                                val cards = DeckUtils.getAllCardsForHalfSuit(hs)
                                val newAssignments = mutableMapOf<Card, String>()
                                val soloTeammate = teammates.singleOrNull()
                                cards.forEach { card ->
                                    if (card in myHand) {
                                        newAssignments[card] = myPlayerId
                                    } else if (soloTeammate != null) {
                                        newAssignments[card] = soloTeammate.id
                                    }
                                }
                                assignments = newAssignments
                                step = ClaimStep.ASSIGN_CARDS
                            }
                        )
                    }
                }
                ClaimStep.ASSIGN_CARDS -> {
                    val hs = selectedHalfSuit ?: return@Column
                    val allCards = DeckUtils.getAllCardsForHalfSuit(hs)
                    val allTeamPlayers = listOf(
                        PlayerInfo(myPlayerId, claimYouLabel, myHand.size, true, false)
                    ) + teammates

                    Text(
                        stringResource(Res.string.claim_assign_instruction),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))

                    @Composable
                    fun CardAssignRow(card: Card) {
                        val isMyCard = card in myHand
                        val currentAssignment = assignments[card]
                        val assignedName = when (currentAssignment) {
                            myPlayerId -> claimYouLabel
                            null -> stringResource(Res.string.claim_unassigned)
                            else -> allTeamPlayers.find { it.id == currentAssignment }?.name ?: "?"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = if (isCompact) 2.dp else 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                card.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (isMyCard) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.height(if (isCompact) 32.dp else 36.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    ) {
                                        Text(
                                            stringResource(Res.string.claim_you_assigned),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .height(if (isCompact) 32.dp else 36.dp)
                                            .clickable { expanded = true }
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(horizontal = 14.dp)
                                        ) {
                                            Text(
                                                "$assignedName \u25be",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (currentAssignment == null)
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        teammates.forEach { player ->
                                            DropdownMenuItem(
                                                text = { Text(player.name, style = MaterialTheme.typography.titleMedium) },
                                                onClick = {
                                                    val newMap = assignments.toMutableMap()
                                                    newMap[card] = player.id
                                                    assignments = newMap
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isCompact) {
                        // 2-column layout: 3 rows of 2 cards each
                        allCards.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { card ->
                                    Box(modifier = Modifier.weight(1f)) { CardAssignRow(card) }
                                }
                                // pad if odd number
                                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    } else {
                        allCards.forEach { card -> CardAssignRow(card) }
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))
                    val allAssigned = allCards.all { assignments.containsKey(it) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step = ClaimStep.SELECT_HALF_SUIT },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.button_back))
                        }
                        Button(
                            onClick = { step = ClaimStep.CONFIRM },
                            enabled = allAssigned,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.button_review))
                        }
                    }
                }
                ClaimStep.CONFIRM -> {
                    val hs = selectedHalfSuit ?: return@Column

                    Text(
                        stringResource(Res.string.claim_claiming, hs.displayName),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
                    Text(
                        stringResource(Res.string.claim_warning),
                        style = MaterialTheme.typography.titleSmall,
                        color = CardRed
                    )
                    Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 16.dp))

                    assignments.entries.groupBy { it.value }.forEach { (playerId, cards) ->
                        val name = if (playerId == myPlayerId) claimYouLabel
                        else teammates.find { it.id == playerId }?.name ?: "?"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                "$name:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(72.dp)
                            )
                            Text(
                                cards.joinToString(", ") { it.key.displayName },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { step = ClaimStep.ASSIGN_CARDS },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(Res.string.button_back))
                        }
                        Button(
                            onClick = {
                                val cardAssignments = mutableMapOf<String, MutableList<Card>>()
                                assignments.forEach { (card, playerId) ->
                                    cardAssignments.getOrPut(playerId) { mutableListOf() }.add(card)
                                }
                                onConfirm(
                                    ClaimDeclaration(
                                        claimerId = myPlayerId,
                                        halfSuit = hs,
                                        cardAssignments = cardAssignments.mapValues { it.value.toList() }
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(stringResource(Res.string.button_claim), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(if (isCompact) 8.dp else 24.dp))
        }
    }
}
