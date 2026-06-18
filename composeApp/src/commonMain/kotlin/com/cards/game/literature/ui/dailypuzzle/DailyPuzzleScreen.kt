package com.cards.game.literature.ui.dailypuzzle

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.deeplink.InviteLink
import com.cards.game.literature.logic.DeckUtils
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.puzzle.DailyPuzzle
import com.cards.game.literature.puzzle.PuzzlePlayer
import com.cards.game.literature.share.Sharer
import com.cards.game.literature.stats.PuzzleStatus
import com.cards.game.literature.ui.game.CardHand
import com.cards.game.literature.ui.game.CardView
import com.cards.game.literature.ui.game.GameLogPanel
import com.cards.game.literature.ui.game.OpponentRow
import com.cards.game.literature.ui.game.TeammateRow
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.viewmodel.DailyPuzzleUiState
import com.cards.game.literature.viewmodel.DailyPuzzleViewModel
import com.cards.game.literature.viewmodel.PlayerInfo
import com.cards.game.literature.viewmodel.PuzzleFeedback
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

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
        onAssign = viewModel::assign,
        onSubmit = viewModel::submit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyPuzzleScreenContent(
    uiState: DailyPuzzleUiState,
    onBack: () -> Unit,
    onSelectHalfSuit: (HalfSuit) -> Unit,
    onAssign: (Card, String) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.home_daily_puzzle), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
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
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PuzzleHeader(uiState)

            val seatName = puzzle.players.associate { it.id to it.name }
            SectionLabel(stringResource(Res.string.daily_puzzle_section_table))
            TeammateRow(puzzle.players.teammates(puzzle.humanTeamId, puzzle.humanSeatId))
            OpponentRow(puzzle.players.opponents(puzzle.humanTeamId))

            SectionLabel(stringResource(Res.string.daily_puzzle_section_moves))
            GameLogPanel(puzzle.events)

            SectionLabel(stringResource(Res.string.daily_puzzle_section_hand))
            CardHand(
                handByHalfSuit = puzzle.myHand.groupBy { DeckUtils.getHalfSuit(it) },
                modifier = Modifier.heightIn(max = 260.dp)
            )

            if (uiState.revealed) {
                PuzzleResult(uiState, puzzle, seatName)
            } else {
                PuzzleSolveSection(uiState, seatName, onSelectHalfSuit, onAssign, onSubmit)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PuzzleHeader(uiState: DailyPuzzleUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.daily_puzzle_number, uiState.puzzleNumber),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(Res.string.daily_puzzle_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (uiState.streak > 0) {
            Text("🔥 ${uiState.streak}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GoldAccent)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PuzzleSolveSection(
    uiState: DailyPuzzleUiState,
    seatName: Map<String, String>,
    onSelectHalfSuit: (HalfSuit) -> Unit,
    onAssign: (Card, String) -> Unit,
    onSubmit: () -> Unit
) {
    SectionLabel(stringResource(Res.string.daily_puzzle_pick_halfsuit))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HalfSuit.entries.forEach { hs ->
            FilterChip(
                selected = uiState.selectedHalfSuit == hs,
                onClick = { onSelectHalfSuit(hs) },
                label = { Text(hs.displayName) }
            )
        }
    }

    val selected = uiState.selectedHalfSuit
    if (selected != null) {
        Spacer(Modifier.height(8.dp))
        SectionLabel(stringResource(Res.string.daily_puzzle_assign, selected.displayName))
        DeckUtils.getAllCardsForHalfSuit(selected).forEach { card ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                CardView(card = card, isSelected = false, onClick = {})
                Spacer(Modifier.width(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    seatName.forEach { (id, name) ->
                        FilterChip(
                            selected = uiState.assignments[card] == id,
                            onClick = { onAssign(card, id) },
                            label = { Text(name) }
                        )
                    }
                }
            }
        }
    }

    feedbackText(uiState)?.let {
        Spacer(Modifier.height(4.dp))
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }

    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(Res.string.daily_puzzle_attempt, uiState.attemptsUsed + 1, uiState.attemptsMax),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = onSubmit,
        enabled = uiState.selectedHalfSuit != null,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(stringResource(Res.string.daily_puzzle_submit), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun feedbackText(uiState: DailyPuzzleUiState): String? = when (uiState.feedback) {
    PuzzleFeedback.ASSIGN_ALL -> stringResource(Res.string.daily_puzzle_assign_all)
    PuzzleFeedback.WRONG_HALF_SUIT -> stringResource(Res.string.daily_puzzle_feedback_halfsuit)
    PuzzleFeedback.WRONG_PLACEMENTS -> stringResource(Res.string.daily_puzzle_feedback_count, uiState.wrongCount)
    PuzzleFeedback.NONE -> null
}

@Composable
private fun PuzzleResult(uiState: DailyPuzzleUiState, puzzle: DailyPuzzle, seatName: Map<String, String>) {
    val solved = uiState.status == PuzzleStatus.SOLVED
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
            .background(
                (if (solved) LightGreen else MaterialTheme.colorScheme.error).copy(alpha = 0.12f),
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(if (solved) Res.string.daily_puzzle_solved else Res.string.daily_puzzle_failed),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = if (solved) LightGreen else MaterialTheme.colorScheme.error
        )
        if (solved) Text("⭐".repeat(uiState.stars) + "☆".repeat(3 - uiState.stars), fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(Res.string.daily_puzzle_answer, puzzle.answer.halfSuit.displayName), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        puzzle.answer.holders.forEach { h ->
            Text(
                stringResource(Res.string.daily_puzzle_holder, h.card.displayName, seatName[h.playerId] ?: h.playerId),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { Sharer.shareText(caption) }, shape = RoundedCornerShape(10.dp)) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.width(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.button_share))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(Res.string.daily_puzzle_come_back),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
}

private fun List<PuzzlePlayer>.teammates(team: String, human: String): List<PlayerInfo> =
    filter { it.teamId == team && it.id != human }
        .map { PlayerInfo(it.id, it.name, it.cardCount, it.cardCount > 0, false) }

private fun List<PuzzlePlayer>.opponents(team: String): List<PlayerInfo> =
    filter { it.teamId != team }
        .map { PlayerInfo(it.id, it.name, it.cardCount, it.cardCount > 0, false) }
