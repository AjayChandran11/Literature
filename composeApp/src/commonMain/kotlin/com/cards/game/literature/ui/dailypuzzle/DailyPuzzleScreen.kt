package com.cards.game.literature.ui.dailypuzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.deeplink.InviteLink
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.isRed
import com.cards.game.literature.model.symbol
import com.cards.game.literature.puzzle.DailyPuzzle
import com.cards.game.literature.share.Sharer
import com.cards.game.literature.stats.PuzzleStatus
import com.cards.game.literature.ui.game.CardView
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.viewmodel.DailyPuzzleUiState
import com.cards.game.literature.viewmodel.DailyPuzzleViewModel
import com.cards.game.literature.viewmodel.PuzzleFeedback
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val CardInk = Color(0xFF1A1A2E) // near-black for black suits on white mini-cards

@Composable
fun DailyPuzzleScreen(
    onBack: () -> Unit,
    viewModel: DailyPuzzleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    DailyPuzzleScreenContent(
        uiState = uiState,
        onBack = onBack,
        onSelectHalfSuit = viewModel::selectHalfSuit,
        onChangeHalfSuit = viewModel::clearHalfSuit,
        onSelectCard = viewModel::selectCard,
        onSubmit = viewModel::submit,
        onHowToSeen = viewModel::markHowToSeen
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyPuzzleScreenContent(
    uiState: DailyPuzzleUiState,
    onBack: () -> Unit,
    onSelectHalfSuit: (HalfSuit) -> Unit,
    onChangeHalfSuit: () -> Unit,
    onSelectCard: (Card) -> Unit,
    onSubmit: () -> Unit,
    onHowToSeen: () -> Unit
) {
    var showHowTo by remember { mutableStateOf(false) }
    var autoOpened by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.loading, uiState.howToSeen) {
        if (!uiState.loading && !uiState.howToSeen && !autoOpened) {
            showHowTo = true; autoOpened = true; onHowToSeen()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.home_daily_puzzle), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showHowTo = true }) {
                        Icon(Icons.Filled.Info, contentDescription = stringResource(Res.string.cd_how_to_play))
                    }
                    if (uiState.streak > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 14.dp)
                        ) {
                            Text("🔥", fontSize = 18.sp)
                            Spacer(Modifier.width(2.dp))
                            Text(
                                "${uiState.streak}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = GoldAccent
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        val puzzle = uiState.puzzle
        if (puzzle == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (uiState.loading) Res.string.daily_puzzle_loading else Res.string.daily_puzzle_load_error),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PuzzleHeader(uiState)
                SectionLabel(stringResource(Res.string.daily_puzzle_section_players))
                PlayerKey(puzzle)
                SectionLabel(stringResource(Res.string.daily_puzzle_section_moves))
                MoveLog(puzzle.events)
                SectionLabel(stringResource(Res.string.daily_puzzle_section_hand))
                CompactHand(puzzle.myHand)

                if (uiState.revealed) {
                    PuzzleResult(uiState, puzzle)
                } else {
                    SolveArea(uiState, puzzle, onSelectHalfSuit, onChangeHalfSuit, onSelectCard, onSubmit)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showHowTo) HowToDialog(onDismiss = { showHowTo = false })
}

// ─── Reusable mini playing card ──────────────────────────────────────────────

@Composable
private fun MiniCard(card: Card, modifier: Modifier = Modifier, dimmed: Boolean = false) {
    val ink = if (card.suit.isRed) CardRed else CardInk
    Column(
        modifier = modifier
            .alpha(if (dimmed) 0.35f else 1f)
            .width(30.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.dp, Color(0x1F000000), RoundedCornerShape(6.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(card.value.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ink, lineHeight = 14.sp)
        Text(card.suit.symbol, fontSize = 11.sp, color = ink, lineHeight = 12.sp)
    }
}

// ─── Header + evidence ───────────────────────────────────────────────────────

@Composable
private fun PuzzleHeader(uiState: DailyPuzzleUiState) {
    Column {
        Text(
            stringResource(Res.string.daily_puzzle_number, uiState.puzzleNumber),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            stringResource(Res.string.daily_puzzle_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerKey(puzzle: DailyPuzzle) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        puzzle.players.forEach { p ->
            val onTeam = p.teamId == puzzle.humanTeamId
            val tint = if (onTeam) MaterialTheme.colorScheme.primary else CardRed
            val isYou = p.id == puzzle.humanSeatId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = 0.12f))
                    .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (isYou) "${p.name} (you)" else p.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Text("${p.cardCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MoveLog(events: List<GameEvent>) {
    val moves = events.filterIsInstance<GameEvent.CardAsked>()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        moves.forEachIndexed { i, m ->
            PuzzleMoveRow(m)
            if (i < moves.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        }
    }
}

@Composable
private fun PuzzleMoveRow(move: GameEvent.CardAsked) {
    val accent = if (move.success) LightGreen else CardRed
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(10.dp))
        MiniCard(move.card)
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(
                if (move.success) Res.string.daily_puzzle_move_got else Res.string.daily_puzzle_move_missed,
                move.askerName, move.targetName
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactHand(hand: List<Card>) {
    val sorted = hand.sortedWith(compareBy({ it.suit.ordinal }, { it.value.rank }))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sorted.forEach { MiniCard(it) }
    }
}

// ─── Solve area (two steps) ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SolveArea(
    uiState: DailyPuzzleUiState,
    puzzle: DailyPuzzle,
    onSelectHalfSuit: (HalfSuit) -> Unit,
    onChangeHalfSuit: () -> Unit,
    onSelectCard: (Card) -> Unit,
    onSubmit: () -> Unit
) {
    val selected = uiState.selectedHalfSuit
    if (selected == null) {
        SectionLabel(stringResource(Res.string.daily_puzzle_step1))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HalfSuit.entries.chunked(4).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { hs ->
                        HalfSuitTile(hs, onClick = { onSelectHalfSuit(hs) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        return
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.daily_puzzle_claiming, selected.displayName), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onChangeHalfSuit) { Text(stringResource(Res.string.daily_puzzle_change)) }
    }

    val teammateName = puzzle.players.first { it.teamId == puzzle.humanTeamId && it.id != puzzle.humanSeatId }.name
    SectionLabel(stringResource(Res.string.daily_puzzle_step2, teammateName))
    Text(stringResource(Res.string.daily_puzzle_step2_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

    val myHand = puzzle.myHand.toSet()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DeckUtils.getAllCardsForHalfSuit(selected).forEach { card ->
            val inHand = card in myHand
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CardView(
                    card = card,
                    isSelected = !inHand && uiState.selectedCard == card,
                    onClick = { if (!inHand) onSelectCard(card) },
                    modifier = Modifier.alpha(if (inHand) 0.4f else 1f)
                )
                Text(
                    if (inHand) stringResource(Res.string.daily_puzzle_you_badge) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    feedbackText(uiState)?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }

    Text(
        stringResource(Res.string.daily_puzzle_attempt, uiState.attemptsUsed + 1, uiState.attemptsMax),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = onSubmit,
        enabled = uiState.selectedCard != null,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(stringResource(Res.string.daily_puzzle_submit), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HalfSuitTile(hs: HalfSuit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val suit = DeckUtils.getAllCardsForHalfSuit(hs).first().suit
    val isLow = hs.name.endsWith("_LOW")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(suit.symbol, fontSize = 26.sp, color = if (suit.isRed) CardRed else MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(2.dp))
        Text(
            if (isLow) "Low" else "High",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun feedbackText(uiState: DailyPuzzleUiState): String? = when (uiState.feedback) {
    PuzzleFeedback.WRONG_HALF_SUIT -> stringResource(Res.string.daily_puzzle_feedback_halfsuit)
    PuzzleFeedback.WRONG_CARD -> stringResource(Res.string.daily_puzzle_feedback_card)
    PuzzleFeedback.NEED_CARD, PuzzleFeedback.NONE -> null
}

// ─── Result ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PuzzleResult(uiState: DailyPuzzleUiState, puzzle: DailyPuzzle) {
    val solved = uiState.status == PuzzleStatus.SOLVED
    val seatName = puzzle.players.associate { it.id to it.name }
    val accent = if (solved) LightGreen else CardRed
    val suffix = (if (solved && uiState.stars > 0) " " + "⭐".repeat(uiState.stars) else "") +
        (if (uiState.streak > 0) " · 🔥${uiState.streak}" else "")
    val base = if (solved) {
        stringResource(Res.string.daily_puzzle_share_solved, uiState.puzzleNumber, uiState.attemptsUsed, uiState.attemptsMax, suffix)
    } else {
        stringResource(Res.string.daily_puzzle_share_failed, uiState.puzzleNumber)
    }
    val caption = "$base\n${InviteLink.PLAY_STORE}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0.05f))))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(if (solved) Res.string.daily_puzzle_solved else Res.string.daily_puzzle_failed),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = accent
        )
        if (solved) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    Text(
                        if (i < uiState.stars) "★" else "☆",
                        fontSize = 30.sp,
                        color = if (i < uiState.stars) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(Res.string.daily_puzzle_answer, puzzle.answer.halfSuit.displayName),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        // The winning claim, shown as the six cards with who holds each.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            puzzle.answer.holders.sortedBy { it.card.value.rank }.forEach { h ->
                val isYou = h.playerId == puzzle.humanSeatId
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MiniCard(h.card)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        seatName[h.playerId] ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isYou) FontWeight.Bold else FontWeight.Normal,
                        color = if (isYou) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { Sharer.shareText(caption) }, shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.button_share))
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(Res.string.daily_puzzle_come_back), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── How to play ─────────────────────────────────────────────────────────────

@Composable
private fun HowToDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.daily_puzzle_howto_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.daily_puzzle_howto_intro))
                Text(stringResource(Res.string.daily_puzzle_howto_s1))
                Text(stringResource(Res.string.daily_puzzle_howto_s2))
                Text(stringResource(Res.string.daily_puzzle_howto_s3))
                Text(
                    stringResource(Res.string.daily_puzzle_howto_outro),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.button_ok)) } }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.width(4.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(GoldAccent)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
