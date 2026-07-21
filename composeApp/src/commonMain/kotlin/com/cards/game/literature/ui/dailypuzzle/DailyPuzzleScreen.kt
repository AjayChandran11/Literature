package com.cards.game.literature.ui.dailypuzzle

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.cards.game.literature.puzzle.HalfSuitClaim
import com.cards.game.literature.puzzle.LocateCard
import com.cards.game.literature.puzzle.PuzzleKind
import com.cards.game.literature.puzzle.PuzzlePlayer
import com.cards.game.literature.puzzle.WastedAsk
import com.cards.game.literature.share.Sharer
import com.cards.game.literature.stats.PuzzleStatus
import com.cards.game.literature.ui.game.CardView
import com.cards.game.literature.ui.stats.AchievementUnlockCard
import com.cards.game.literature.ui.theme.CardFaceInk
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.viewmodel.DailyPuzzleUiState
import com.cards.game.literature.viewmodel.DailyPuzzleViewModel
import com.cards.game.literature.viewmodel.PuzzleFeedback
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import com.cards.game.literature.notifications.RequestNotificationPermissionOnce
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun DailyPuzzleScreen(
    onBack: () -> Unit,
    viewModel: DailyPuzzleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // A daily-puzzle player may never open the online lobby, so prompt for the notification grant
    // here too — the reminder can't be delivered without it on Android 13+ / iOS.
    RequestNotificationPermissionOnce()
    DailyPuzzleScreenContent(
        uiState = uiState,
        onBack = onBack,
        onSelectHalfSuit = viewModel::selectHalfSuit,
        onChangeHalfSuit = viewModel::clearHalfSuit,
        onSelectCard = viewModel::selectCard,
        onSelectSeat = viewModel::selectSeat,
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
    onSelectSeat: (String) -> Unit,
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

    // Evidence starts open so a first-time solver reads the story, then folds away once they
    // commit (Claim step 1) or finish the day. On re-entry to an already solved/failed puzzle it
    // starts folded so the result + Share are visible without scrolling — purely presentational.
    var evidenceExpanded by remember { mutableStateOf(!uiState.revealed) }
    LaunchedEffect(uiState.selectedHalfSuit) {
        if (uiState.selectedHalfSuit != null) evidenceExpanded = false
    }
    LaunchedEffect(uiState.revealed) {
        if (uiState.revealed) evidenceExpanded = false
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
            // ONE scroll only (a small-screen fallback). No nested same-direction
            // scroll anywhere — that was the old "shaky" jank.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Overline(stringResource(Res.string.daily_puzzle_number, uiState.puzzleNumber))
                CaseLine(uiState, puzzle, onChangeHalfSuit)
                EvidenceCard(puzzle.events, expanded = evidenceExpanded, onToggle = { evidenceExpanded = !evidenceExpanded })
                SeatsStrip(puzzle)

                if (uiState.revealed) {
                    PuzzleResult(uiState, puzzle)
                } else {
                    QuestionCard(uiState, puzzle, onSelectHalfSuit, onSelectCard, onSelectSeat, onSubmit)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showHowTo) HowToDialog(uiState.puzzle?.kind ?: PuzzleKind.CLAIM, onDismiss = { showHowTo = false })
}

// ─── Header + narrative ───────────────────────────────────────────────────────

@Composable
private fun Overline(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CaseLine(uiState: DailyPuzzleUiState, puzzle: DailyPuzzle, onChange: () -> Unit) {
    when (puzzle.kind) {
        PuzzleKind.CLAIM -> ClaimCaseLine(uiState, puzzle, onChange)
        PuzzleKind.LOCATE -> NarrativeLine(stringResource(Res.string.daily_puzzle_locate_case_line))
        PuzzleKind.WASTED_ASK -> NarrativeLine(stringResource(Res.string.daily_puzzle_wasted_case_line))
    }
}

@Composable
private fun NarrativeLine(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ClaimCaseLine(uiState: DailyPuzzleUiState, puzzle: DailyPuzzle, onChange: () -> Unit) {
    val selected = uiState.selectedHalfSuit
    if (selected == null || uiState.revealed) {
        Text(
            stringResource(Res.string.daily_puzzle_case_line, teammateName(puzzle)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    } else {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GoldAccent.copy(alpha = 0.18f))
                    .border(1.dp, GoldAccent, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    stringResource(Res.string.daily_puzzle_claiming, selected.displayName),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(onClick = onChange) { Text(stringResource(Res.string.daily_puzzle_change)) }
        }
    }
}

// ─── Evidence (collapsible, never its own scroll) ─────────────────────────────

@Composable
private fun EvidenceCard(events: List<GameEvent>, expanded: Boolean, onToggle: () -> Unit) {
    val moves = events.filterIsInstance<GameEvent.CardAsked>()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .animateContentSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.daily_puzzle_evidence),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (!expanded) {
                Text(
                    stringResource(Res.string.daily_puzzle_evidence_collapsed, moves.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            moves.forEachIndexed { i, m ->
                PuzzleMoveRow(m)
                if (i < moves.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            }
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

// ─── Seats (one compact, non-scrolling row) ───────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeatsStrip(puzzle: DailyPuzzle) {
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

// ─── The hero: one focused question (half-suit → clue board) ──────────────────

@Composable
private fun QuestionCard(
    uiState: DailyPuzzleUiState,
    puzzle: DailyPuzzle,
    onSelectHalfSuit: (HalfSuit) -> Unit,
    onSelectCard: (Card) -> Unit,
    onSelectSeat: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        when (puzzle.kind) {
            PuzzleKind.CLAIM -> AnimatedContent(
                targetState = uiState.selectedHalfSuit == null,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                label = "solve-step"
            ) { pickingHalfSuit ->
                if (pickingHalfSuit) {
                    StepOne(onSelectHalfSuit)
                } else {
                    StepTwo(uiState, puzzle, onSelectCard, onSubmit)
                }
            }
            PuzzleKind.LOCATE -> SeatPickStep(
                uiState = uiState,
                puzzle = puzzle,
                answerCard = (puzzle.answer as LocateCard).card,
                prompt = stringResource(Res.string.daily_puzzle_locate_q),
                pickHint = stringResource(Res.string.daily_puzzle_locate_pick),
                // Every seat but your own — you already know your own hand.
                seats = puzzle.players.filter { it.id != puzzle.humanSeatId },
                onSelectSeat = onSelectSeat,
                onSubmit = onSubmit
            )
            PuzzleKind.WASTED_ASK -> SeatPickStep(
                uiState = uiState,
                puzzle = puzzle,
                answerCard = (puzzle.answer as WastedAsk).card,
                prompt = stringResource(Res.string.daily_puzzle_wasted_q),
                pickHint = stringResource(Res.string.daily_puzzle_wasted_pick),
                // Only opponents are askable.
                seats = puzzle.players.filter { it.teamId != puzzle.humanTeamId },
                onSelectSeat = onSelectSeat,
                onSubmit = onSubmit
            )
        }
    }
}

/**
 * The one-tap hero shared by LOCATE and WASTED_ASK: a focused card, a row of selectable seat
 * chips, and Submit. [seats] is the candidate set (all but you for LOCATE; opponents for WASTED_ASK).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeatPickStep(
    uiState: DailyPuzzleUiState,
    puzzle: DailyPuzzle,
    answerCard: Card,
    prompt: String,
    pickHint: String,
    seats: List<PuzzlePlayer>,
    onSelectSeat: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PromptHeader(prompt)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FocusCard(answerCard)
        }
        Text(
            pickHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SeatPickRow(
            puzzle = puzzle,
            seats = seats,
            selectedSeatId = uiState.selectedSeatId,
            onSelect = onSelectSeat
        )

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
            enabled = uiState.selectedSeatId != null,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(stringResource(Res.string.daily_puzzle_submit_seat), fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeatPickRow(
    puzzle: DailyPuzzle,
    seats: List<PuzzlePlayer>,
    selectedSeatId: String?,
    onSelect: (String) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        seats.forEach { p ->
            val onTeam = p.teamId == puzzle.humanTeamId
            val tint = if (onTeam) MaterialTheme.colorScheme.primary else CardRed
            val selected = p.id == selectedSeatId
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = if (selected) 0.28f else 0.10f))
                    .border(if (selected) 2.dp else 1.dp, tint.copy(alpha = if (selected) 1f else 0.4f), RoundedCornerShape(50))
                    .clickable { onSelect(p.id) }
                    .semantics(mergeDescendants = true) { this.selected = p.id == selectedSeatId }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(tint))
                Spacer(Modifier.width(8.dp))
                Text(
                    p.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Text("${p.cardCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PromptHeader(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(4.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(GoldAccent))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StepOne(onSelectHalfSuit: (HalfSuit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PromptHeader(stringResource(Res.string.daily_puzzle_step1_q))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HalfSuit.entries.chunked(4).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowItems.forEach { hs ->
                        HalfSuitTile(hs, onClick = { onSelectHalfSuit(hs) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepTwo(
    uiState: DailyPuzzleUiState,
    puzzle: DailyPuzzle,
    onSelectCard: (Card) -> Unit,
    onSubmit: () -> Unit
) {
    val selected = uiState.selectedHalfSuit ?: return
    val myHand = puzzle.myHand.toSet()
    val cards = DeckUtils.getAllCardsForHalfSuit(selected)
    val myCards = cards.filter { it in myHand }
    val candidates = cards.filter { it !in myHand } // pulled cards + the one hidden card

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PromptHeader(stringResource(Res.string.daily_puzzle_board_title, selected.displayName))

        // The clue board: your cards placed as facts on the left, the teammate's one
        // hidden card as a glowing gap on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            BoardColumn(
                label = stringResource(Res.string.daily_puzzle_you_badge),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    myCards.forEach { MiniCard(it, placedTint = MaterialTheme.colorScheme.primary) }
                }
            }
            BoardColumn(
                label = teammateName(puzzle),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            ) {
                GapSlot(uiState.selectedCard)
            }
        }

        RuledOutRow(puzzle)

        Text(
            stringResource(Res.string.daily_puzzle_rail_prompt, teammateName(puzzle)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            candidates.forEach { card ->
                CardView(
                    card = card,
                    isSelected = uiState.selectedCard == card,
                    onClick = { onSelectCard(card) }
                )
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
}

@Composable
private fun BoardColumn(label: String, tint: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
private fun GapSlot(selected: Card?) {
    if (selected != null) {
        // Mirror the player's choice in the gap, highlighted, so the board reads as solved-in-progress.
        CardView(card = selected, isSelected = true, onClick = {})
        return
    }
    val gold = GoldAccent
    val transition = rememberInfiniteTransition(label = "gap")
    val pulse by transition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "gap-alpha"
    )
    Box(
        modifier = Modifier
            .width(60.dp)
            .height(80.dp)
            .drawBehind {
                drawRoundRect(
                    color = gold.copy(alpha = pulse),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text("?", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = gold.copy(alpha = pulse))
    }
}

@Composable
private fun RuledOutRow(puzzle: DailyPuzzle) {
    val opponents = puzzle.players.filter { it.teamId != puzzle.humanTeamId }
    if (opponents.size < 2) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(CardRed))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(Res.string.daily_puzzle_ruled_out, opponents[0].name, opponents[1].name, teammateName(puzzle)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Reusable mini playing card ──────────────────────────────────────────────

/** Localized screen-reader label for a card, e.g. "9 of Hearts" (matches CardView's). */
@Composable
private fun cardContentDescription(card: Card): String =
    stringResource(Res.string.cd_card, card.value.displayName, card.suit.name.lowercase().replaceFirstChar { it.uppercase() })

/** A larger, NON-interactive playing card presenting the card in question (LOCATE / WASTED_ASK). */
@Composable
private fun FocusCard(card: Card) {
    val ink = if (card.suit.isRed) CardRed else CardFaceInk
    val desc = cardContentDescription(card)
    Column(
        modifier = Modifier
            .width(60.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0x1F000000), RoundedCornerShape(8.dp))
            .clearAndSetSemantics { contentDescription = desc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(card.value.displayName, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = ink, lineHeight = 28.sp)
        Text(card.suit.symbol, fontSize = 22.sp, color = ink, lineHeight = 24.sp)
    }
}

@Composable
private fun MiniCard(card: Card, modifier: Modifier = Modifier, dimmed: Boolean = false, placedTint: Color? = null) {
    val ink = if (card.suit.isRed) CardRed else CardFaceInk
    val border = placedTint?.copy(alpha = 0.6f) ?: Color(0x1F000000)
    val desc = cardContentDescription(card)
    Column(
        modifier = modifier
            .alpha(if (dimmed) 0.35f else 1f)
            .width(30.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(if (placedTint != null) 1.5.dp else 1.dp, border, RoundedCornerShape(6.dp))
            .clearAndSetSemantics { contentDescription = desc },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(card.value.displayName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ink, lineHeight = 14.sp)
        Text(card.suit.symbol, fontSize = 11.sp, color = ink, lineHeight = 12.sp)
    }
}

@Composable
private fun HalfSuitTile(hs: HalfSuit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val suit = DeckUtils.getAllCardsForHalfSuit(hs).first().suit
    val isLow = hs.name.endsWith("_LOW")
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
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
    PuzzleFeedback.WRONG_SEAT -> stringResource(Res.string.daily_puzzle_feedback_seat)
    PuzzleFeedback.NEED_CARD, PuzzleFeedback.NONE -> null
}

private fun teammateName(puzzle: DailyPuzzle): String =
    puzzle.players.first { it.teamId == puzzle.humanTeamId && it.id != puzzle.humanSeatId }.name

// ─── Result ──────────────────────────────────────────────────────────────────

@Composable
private fun PuzzleResult(uiState: DailyPuzzleUiState, puzzle: DailyPuzzle) {
    when (val answer = puzzle.answer) {
        is HalfSuitClaim -> ResultScaffold(uiState) { ClaimReveal(puzzle, answer) }
        is LocateCard -> ResultScaffold(uiState) { SeatReveal(puzzle, answer.card, answer.seatId, locate = true) }
        is WastedAsk -> ResultScaffold(uiState) { SeatReveal(puzzle, answer.card, answer.seatId, locate = false) }
    }
}

/** Shared result chrome: headline, the win stars + streak "moment", then [reveal], then share. */
@Composable
private fun ResultScaffold(uiState: DailyPuzzleUiState, reveal: @Composable () -> Unit) {
    val solved = uiState.status == PuzzleStatus.SOLVED
    val accent = if (solved) LightGreen else CardRed
    // Spoiler-safe share line (no answer leaked) — identical across kinds.
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
            // Stars pop in one-by-one — the win "moment". Skipped in @Preview so the static
            // panes show the finished state instead of catching the entrance at scale 0.
            val inPreview = LocalInspectionMode.current
            val starScale = remember { List(3) { Animatable(if (inPreview) 1f else 0f) } }
            LaunchedEffect(Unit) {
                if (inPreview) return@LaunchedEffect
                starScale.forEachIndexed { i, a ->
                    launch {
                        delay(120L * i)
                        a.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    Text(
                        if (i < uiState.stars) "★" else "☆",
                        fontSize = 30.sp,
                        color = if (i < uiState.stars) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.scale(starScale[i].value)
                    )
                }
            }
            // Streak chip springs in just after the stars.
            if (uiState.streak > 0) {
                Spacer(Modifier.height(10.dp))
                var showStreak by remember { mutableStateOf(inPreview) }
                LaunchedEffect(Unit) { if (!inPreview) { delay(120L * 3 + 140L); showStreak = true } }
                AnimatedVisibility(
                    visible = showStreak,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(GoldAccent.copy(alpha = 0.18f))
                            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text("🔥", fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(Res.string.daily_puzzle_streak_chip, uiState.streak),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        reveal()
        if (uiState.newlyUnlocked.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            AchievementUnlockCard(uiState.newlyUnlocked)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClaimReveal(puzzle: DailyPuzzle, answer: HalfSuitClaim) {
    val seatName = puzzle.players.associate { it.id to it.name }
    Text(
        stringResource(Res.string.daily_puzzle_answer, answer.halfSuit.displayName),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(8.dp))
    // The winning claim, shown as the six cards with who holds each.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        answer.holders.sortedBy { it.card.value.rank }.forEach { h ->
            val isYou = h.playerId == puzzle.humanSeatId
            val isHidden = h.card == answer.hiddenCard
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MiniCard(h.card, placedTint = if (isHidden) GoldAccent else null)
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
}

/** LOCATE / WASTED_ASK reveal: the card, its true seat, and a one-line explanation. */
@Composable
private fun SeatReveal(puzzle: DailyPuzzle, card: Card, seatId: String, locate: Boolean) {
    val seatName = puzzle.players.associate { it.id to it.name }[seatId] ?: ""
    Text(
        stringResource(
            if (locate) Res.string.daily_puzzle_locate_answer else Res.string.daily_puzzle_wasted_answer,
            if (locate) card.displayName else seatName,
            if (locate) seatName else card.displayName
        ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MiniCard(card, placedTint = GoldAccent)
        Spacer(Modifier.height(2.dp))
        Text(seatName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─── How to play ─────────────────────────────────────────────────────────────

@Composable
private fun HowToDialog(kind: PuzzleKind, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(howToTitle(kind)), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (kind) {
                    PuzzleKind.CLAIM -> {
                        Text(stringResource(Res.string.daily_puzzle_howto_intro))
                        Text(stringResource(Res.string.daily_puzzle_howto_s1))
                        Text(stringResource(Res.string.daily_puzzle_howto_s2))
                        Text(stringResource(Res.string.daily_puzzle_howto_s3))
                        HowToOutro(stringResource(Res.string.daily_puzzle_howto_outro))
                    }
                    PuzzleKind.LOCATE -> {
                        Text(stringResource(Res.string.daily_puzzle_howto_locate_intro))
                        Text(stringResource(Res.string.daily_puzzle_howto_locate_s1))
                        Text(stringResource(Res.string.daily_puzzle_howto_locate_s2))
                        HowToOutro(stringResource(Res.string.daily_puzzle_howto_locate_outro))
                    }
                    PuzzleKind.WASTED_ASK -> {
                        Text(stringResource(Res.string.daily_puzzle_howto_wasted_intro))
                        Text(stringResource(Res.string.daily_puzzle_howto_wasted_s1))
                        Text(stringResource(Res.string.daily_puzzle_howto_wasted_s2))
                        HowToOutro(stringResource(Res.string.daily_puzzle_howto_wasted_outro))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.button_ok)) } }
    )
}

private fun howToTitle(kind: PuzzleKind) = when (kind) {
    PuzzleKind.CLAIM -> Res.string.daily_puzzle_howto_title
    PuzzleKind.LOCATE -> Res.string.daily_puzzle_howto_locate_title
    PuzzleKind.WASTED_ASK -> Res.string.daily_puzzle_howto_wasted_title
}

@Composable
private fun HowToOutro(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
}
