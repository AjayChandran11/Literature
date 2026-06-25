package com.cards.game.literature.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.deeplink.DeepLinkHandler
import com.cards.game.literature.preferences.SessionStore
import com.cards.game.literature.preferences.TutorialPrefs
import com.cards.game.literature.stats.PlayerStats
import com.cards.game.literature.stats.PuzzleStatus
import com.cards.game.literature.stats.PuzzleStore
import com.cards.game.literature.stats.StatsStore
import com.cards.game.literature.stats.currentEpochDay
import com.cards.game.literature.ui.stats.StreakValue
import com.cards.game.literature.ui.common.WindowSize.isCompactHeight
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LiteratureTheme
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartGame: (playerName: String, playerCount: Int, difficulty: BotDifficulty) -> Unit,
    onPlayOnline: (playerName: String) -> Unit = {},
    onJoinRoom: (playerName: String, roomCode: String) -> Unit = { _, _ -> },
    onOpenStats: () -> Unit = {},
    onOpenDailyPuzzle: () -> Unit = {}
) {
    val session = koinInject<SessionStore>()
    var playerName by rememberSaveable { mutableStateOf(session.playerName) }
    val pendingInvite by DeepLinkHandler.pendingRoomCode.collectAsState()
    var showSetupDialog by remember { mutableStateOf(false) }
    var showOnlineGateDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val onBackground = MaterialTheme.colorScheme.onBackground

    // Flag the Daily Puzzle button with a "!" alert badge on the FIRST Home open of the day
    // only — and never again that day (even after visiting another screen and coming back).
    // Decided once per Home entry from a snapshot via plain remember (NOT rememberSaveable, which
    // NavHost would restore): returning from another destination recomposes Home fresh and re-reads
    // the now-persisted flag, so it stays off. We mark the day shown as soon as it's displayed.
    val today = currentEpochDay()
    val showPuzzleHighlight = remember {
        val p = PuzzleStore.today(today)
        p.status != PuzzleStatus.SOLVED && p.status != PuzzleStatus.FAILED && p.readyHintShownDay != today
    }
    LaunchedEffect(Unit) { if (showPuzzleHighlight) PuzzleStore.markReadyHintShown(today) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Corner suit symbols
        val cornerSuits = listOf(
            stringResource(Res.string.suit_spades),
            stringResource(Res.string.suit_hearts),
            stringResource(Res.string.suit_diamonds),
            stringResource(Res.string.suit_clubs)
        )
        val cornerColors = listOf(onBackground, CardRed, CardRed, onBackground)
        val cornerAlignments = listOf(
            Alignment.TopStart, Alignment.TopEnd,
            Alignment.BottomEnd, Alignment.BottomStart
        )
        cornerSuits.forEachIndexed { i, suit ->
            Text(
                suit,
                fontSize = 52.sp,
                color = cornerColors[i].copy(alpha = 0.12f),
                modifier = Modifier
                    .align(cornerAlignments[i])
                    .padding(40.dp)
                    .clearAndSetSemantics { }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.suits_display),
                fontSize = 48.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.home_title),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.home_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Room invite from a deep link — prompt the player to join.
            pendingInvite?.let { code ->
                Spacer(modifier = Modifier.height(24.dp))
                RoomInviteCard(
                    roomCode = code,
                    canJoin = playerName.isNotBlank(),
                    onJoin = {
                        DeepLinkHandler.consume()
                        onJoinRoom(playerName.trim(), code)
                    },
                    onDismiss = { DeepLinkHandler.consume() }
                )
            }

            val stats by StatsStore.stats.collectAsState()
            if (stats.gamesPlayed > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                HomeStatsCard(stats = stats)
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }

            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it; session.playerName = it },
                label = { Text(stringResource(Res.string.home_player_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                    cursorColor = MaterialTheme.colorScheme.secondary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { showSetupDialog = true },
                enabled = playerName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.outline
                )
            ) {
                Text(stringResource(Res.string.home_new_game), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Two secondary "modes" side by side, below the primary New Game CTA.
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeModeTile(
                    icon = Icons.Filled.Public,
                    label = stringResource(Res.string.home_play_online),
                    enabled = playerName.isNotBlank(),
                    onClick = {
                        if (!TutorialPrefs.isFirstGameCompleted()) {
                            showOnlineGateDialog = true
                        } else {
                            onPlayOnline(playerName.trim())
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                BadgedBox(
                    modifier = Modifier.weight(1f),
                    badge = {
                        if (showPuzzleHighlight) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ) {
                                Text(stringResource(Res.string.daily_puzzle_badge_alert))
                            }
                        }
                    }
                ) {
                    HomeModeTile(
                        icon = Icons.Filled.Extension,
                        label = stringResource(Res.string.home_daily_puzzle),
                        onClick = onOpenDailyPuzzle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpenStats) {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(Res.string.home_my_stats),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showSettingsSheet = true }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(Res.string.home_settings),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(onDismiss = { showSettingsSheet = false })
    }

    if (showSetupDialog) {
        GameSetupDialog(
            onDismiss = { showSetupDialog = false },
            onConfirm = { playerCount, difficulty ->
                showSetupDialog = false
                onStartGame(playerName.trim(), playerCount, difficulty)
            }
        )
    }

    if (showOnlineGateDialog) {
        AlertDialog(
            onDismissRequest = { showOnlineGateDialog = false },
            title = {
                Text(
                    stringResource(Res.string.dialog_online_gate_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(stringResource(Res.string.dialog_online_gate_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showOnlineGateDialog = false
                        showSetupDialog = true
                    }
                ) {
                    Text(stringResource(Res.string.dialog_online_gate_play_offline))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOnlineGateDialog = false
                        // Player chose to skip the offline tutorial — treat onboarding
                        // as done so the gate dialog and in-game tooltips don't reappear.
                        TutorialPrefs.markFirstGameCompleted()
                        onPlayOnline(playerName.trim())
                    }
                ) {
                    Text(stringResource(Res.string.dialog_online_gate_continue))
                }
            }
        )
    }
}

/**
 * A single golden line that travels around the button's rounded border while [active], with a
 * sparkle at its leading point. Drawn over the content (above the button's own outline) and inset
 * so it isn't clipped. No-op when inactive.
 *
 * NOTE: not currently wired into the Home button (replaced by a simpler "!" alert badge because
 * the motion felt too busy) — kept, together with [DailyPuzzleReadyHighlightPreview], for reuse.
 */
@Composable
private fun Modifier.dailyPuzzleReadyHighlight(active: Boolean): Modifier {
    if (!active) return this
    val gold = MaterialTheme.colorScheme.secondary
    val transition = rememberInfiniteTransition(label = "puzzle-ready")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f, // fraction of the perimeter the line's head has travelled
        animationSpec = infiniteRepeatable(tween(durationMillis = 2200, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit"
    )
    val sparkle by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 650, easing = LinearEasing), RepeatMode.Reverse),
        label = "sparkle"
    )
    val borderPath = remember { Path() }
    val linePath = remember { Path() }
    val measure = remember { PathMeasure() }
    return drawWithContent {
        drawContent()
        val strokePx = 3.dp.toPx()
        val inset = strokePx / 2f + 1.dp.toPx()
        val corner = (12.dp.toPx() - inset).coerceAtLeast(0f)
        borderPath.reset()
        borderPath.addRoundRect(
            RoundRect(inset, inset, size.width - inset, size.height - inset, CornerRadius(corner, corner))
        )
        measure.setPath(borderPath, true)
        val total = measure.length
        if (total <= 0f) return@drawWithContent

        val lineLen = total * 0.22f          // a single short arc, ~22% of the perimeter
        val head = (progress * total).coerceIn(0f, total)
        val tailStart = head - lineLen
        linePath.reset()
        if (tailStart >= 0f) {
            measure.getSegment(tailStart, head, linePath, true)
        } else {
            // wrap across the 0/total seam
            measure.getSegment(total + tailStart, total, linePath, true)
            measure.getSegment(0f, head, linePath, true)
        }
        // soft glow under the line, then the crisp golden line itself
        drawPath(linePath, color = gold.copy(alpha = 0.22f), style = Stroke(width = strokePx * 2.6f, cap = StrokeCap.Round))
        drawPath(linePath, color = gold, style = Stroke(width = strokePx, cap = StrokeCap.Round))

        // sparkle at the leading point
        val headPos = measure.getPosition(head)
        val rayLen = strokePx * 3.2f * sparkle
        drawCircle(color = gold.copy(alpha = 0.28f), radius = strokePx * 3.0f * sparkle, center = headPos)
        drawCircle(color = Color.White, radius = strokePx * 0.95f, center = headPos)
        val ray = gold.copy(alpha = 0.9f)
        drawLine(ray, Offset(headPos.x - rayLen, headPos.y), Offset(headPos.x + rayLen, headPos.y), strokeWidth = strokePx * 0.5f, cap = StrokeCap.Round)
        drawLine(ray, Offset(headPos.x, headPos.y - rayLen), Offset(headPos.x, headPos.y + rayLen), strokeWidth = strokePx * 0.5f, cap = StrokeCap.Round)
    }
}

/**
 * Preview of the moving golden line + sparkle. Open Android Studio's *Interactive Preview*
 * (or the Animation Preview) on this to watch the line travel around the border.
 */
@Preview(name = "Daily Puzzle button — ready highlight", showBackground = true)
@Composable
private fun DailyPuzzleReadyHighlightPreview() {
    LiteratureTheme {
        Box(Modifier.padding(24.dp)) {
            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .dailyPuzzleReadyHighlight(active = true),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(stringResource(Res.string.home_daily_puzzle), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Preview of the two secondary mode tiles, with the "!" badge on Daily Puzzle. */
@Preview(name = "Home mode tiles — Online + Daily (!)", showBackground = true)
@Composable
private fun HomeModeTilesPreview() {
    LiteratureTheme {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeModeTile(
                icon = Icons.Filled.Public,
                label = stringResource(Res.string.home_play_online),
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            BadgedBox(
                modifier = Modifier.weight(1f),
                badge = {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ) {
                        Text(stringResource(Res.string.daily_puzzle_badge_alert))
                    }
                }
            ) {
                HomeModeTile(
                    icon = Icons.Filled.Extension,
                    label = stringResource(Res.string.home_daily_puzzle),
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun RoomInviteCard(
    roomCode: String,
    canJoin: Boolean,
    onJoin: () -> Unit,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(16.dp))
            .background(primary.copy(alpha = 0.10f))
            .border(1.dp, primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.home_invite_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.cd_dismiss_invite),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.home_invite_room_code, roomCode),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onJoin,
            enabled = canJoin,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primary,
                disabledContainerColor = MaterialTheme.colorScheme.outline
            )
        ) {
            Text(
                stringResource(Res.string.home_invite_join),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (!canJoin) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.home_invite_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun HomeStatsCard(stats: PlayerStats) {
    val cd = stringResource(Res.string.cd_home_stats_card)
    // Not clickable by design — stats are opened via the "My Stats" button only.
    Row(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = cd },
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeStatMini("${stats.gamesPlayed}", stringResource(Res.string.stats_games), Modifier.weight(1f))
        HomeStatMini("${(stats.winRate * 100).toInt()}%", stringResource(Res.string.stats_wins), Modifier.weight(1f))
        HomeStreakMini(stats, Modifier.weight(1f))
    }
}

@Composable
private fun HomeStreakMini(stats: PlayerStats, modifier: Modifier = Modifier) {
    // Adaptive, no guilt-trip: show the live streak when active, else fall back to the
    // best streak (a proud number rather than a "-"). The Home strip only shows once
    // gamesPlayed > 0, so best is always >= 1 here and "-" never appears on Home.
    val current = stats.displayedDailyStreak()
    val showingBest = current == 0 && stats.bestDailyStreak > 0
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        StreakValue(
            streak = if (current > 0) current else stats.bestDailyStreak,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            stringResource(if (showingBest) Res.string.home_streak_best else Res.string.stats_current_streak),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeStatMini(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A compact, icon-over-label secondary action tile (Play Online / Daily Puzzle). */
@Composable
private fun HomeModeTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    // Mirror Material's disabled OutlinedButton: muted grey content + very faint border when
    // disabled (NOT colorScheme.outline, which is a vivid green in the light theme and reads active).
    val contentColor =
        if (enabled) MaterialTheme.colorScheme.secondary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val borderColor =
        if (enabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
fun GameSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, BotDifficulty) -> Unit,
    confirmLabel: String = stringResource(Res.string.button_start_game),
    allowEightPlayers: Boolean = false,
    showDifficulty: Boolean = true
) {
    val windowInfo = currentWindowAdaptiveInfo()
    val isCompact = windowInfo.isCompactHeight
    var selectedCount by remember { mutableIntStateOf(4) }
    var selectedDifficulty by remember { mutableStateOf(BotDifficulty.MEDIUM) }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val difficultyLabels = mapOf(
        BotDifficulty.EASY to Pair(stringResource(Res.string.difficulty_easy), stringResource(Res.string.difficulty_easy_desc)),
        BotDifficulty.MEDIUM to Pair(stringResource(Res.string.difficulty_medium), stringResource(Res.string.difficulty_medium_desc)),
        BotDifficulty.HARD to Pair(stringResource(Res.string.difficulty_hard), stringResource(Res.string.difficulty_hard_desc))
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = if (isCompact) Modifier.fillMaxHeight(0.9f) else Modifier
        ) {
            Column(
                modifier = Modifier
                    .padding(if (isCompact) 16.dp else 28.dp)
                    .then(if (isCompact) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = stringResource(Res.string.suits_display),
                    fontSize = if (isCompact) 18.sp else 22.sp,
                    color = secondary,
                    letterSpacing = 6.sp
                )
                Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
                Text(
                    text = stringResource(Res.string.game_setup_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Text(
                    text = stringResource(Res.string.game_setup_players_question),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 24.dp))

                // Player count cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    (if (allowEightPlayers) listOf(4, 6, 8) else listOf(4, 6)).forEach { count ->
                        val isSelected = selectedCount == count
                        val teams = count / 2

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) primary.copy(alpha = 0.12f)
                                    else surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) primary else primary.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedCount = count }
                                .padding(vertical = if (isCompact) 8.dp else 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$count",
                                    fontSize = if (isCompact) 20.sp else 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) primary else onSurface
                                )
                                Text(
                                    text = stringResource(Res.string.game_setup_players_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 6.dp))
                                Text(
                                    text = stringResource(Res.string.game_setup_teams_format, teams, teams),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Bot difficulty selector
                if (showDifficulty) {
                    Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 24.dp))

                    Text(
                        text = stringResource(Res.string.game_setup_difficulty_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BotDifficulty.entries.forEach { difficulty ->
                            val isSelected = selectedDifficulty == difficulty
                            val (label, desc) = difficultyLabels[difficulty] ?: Pair(difficulty.label, "")

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) primary.copy(alpha = 0.12f)
                                        else surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) primary else primary.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { selectedDifficulty = difficulty }
                                    .padding(vertical = if (isCompact) 6.dp else 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = label,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) primary else onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 28.dp))

                // Action buttons
                Button(
                    onClick = { onConfirm(selectedCount, selectedDifficulty) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 44.dp else 52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text(
                        confirmLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(Res.string.button_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
