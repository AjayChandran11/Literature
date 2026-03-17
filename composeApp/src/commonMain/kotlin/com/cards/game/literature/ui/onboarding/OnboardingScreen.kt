package com.cards.game.literature.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.cards.game.literature.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground

    // Intercept back on pages > 0 to go back a page; on page 0 do nothing
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> DeckPage(isActive = pagerState.currentPage == 1)
                2 -> TeamsPage(isActive = pagerState.currentPage == 2)
                3 -> AskPage()
                4 -> ClaimPage(onFinish = onFinish, isActive = pagerState.currentPage == 4)
            }
        }

        // Skip button — hidden on last page
        AnimatedVisibility(
            visible = pagerState.currentPage < 4,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 16.dp, top = 8.dp)
        ) {
            TextButton(onClick = onFinish) {
                Text("Skip", color = onBackground.copy(alpha = 0.5f), fontSize = 14.sp)
            }
        }

        // Bottom controls: hint + indicator + Next button
        if (pagerState.currentPage < 4) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Swipe hint — only on welcome page
                if (pagerState.currentPage == 0) {
                    var hintVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(2060); hintVisible = true }
                    AnimatedVisibility(
                        visible = hintVisible,
                        enter = fadeIn(tween(500))
                    ) {
                        PulsingHint(color = onBackground)
                    }
                }
                PagerIndicator(
                    pageCount = 5,
                    currentPage = pagerState.currentPage
                )
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp)
                ) {
                    Text(
                        "Next  →",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            // Indicator only on last page (button is inside ClaimPage)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 140.dp)
            ) {
                PagerIndicator(pageCount = 5, currentPage = 4)
            }
        }
    }
}

// ─── Page Indicator ─────────────────────────────────────────────────────────

@Composable
private fun PagerIndicator(pageCount: Int, currentPage: Int) {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isSelected) 28.dp else 8.dp,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "dot"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) GoldAccent else onBackground.copy(alpha = 0.25f)
                    )
            )
        }
    }
}

// ─── Page 1: Welcome ────────────────────────────────────────────────────────

// Isolated composable — shimmer state reads stay here, never recompose parent
@Composable
private fun ShimmerTitle() {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmer.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = LinearEasing),
            RepeatMode.Restart
        ), label = "shimmerX"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(GoldAccent, Color(0xFFFFF9E6), GoldAccent),
        start = Offset((shimmerOffset - 0.15f) * 900f, 0f),
        end   = Offset((shimmerOffset + 0.15f) * 900f, 0f)
    )
    Text(
        "Literature",
        style = TextStyle(
            brush = shimmerBrush,
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold
        )
    )
}

// Isolated composable — pulse alpha reads stay here, never recompose parent
@Composable
private fun PulsingHint(color: Color) {
    val hintAlpha by rememberInfiniteTransition(label = "hint")
        .animateFloat(
            initialValue = 0.35f, targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "hintAlpha"
        )
    Text(
        "Swipe to learn how to play →",
        fontSize = 13.sp,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.graphicsLayer { alpha = hintAlpha }
    )
}

@Composable
private fun WelcomePage() {
    // Individual states instead of mutableStateListOf — avoids over-notification
    val suitVisible = remember { Array(4) { mutableStateOf(false) } }
    var titleVisible by remember { mutableStateOf(false) }
    var subtitleVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        repeat(4) { i ->
            delay(180L)
            suitVisible[i].value = true
        }
        delay(200); titleVisible = true
        delay(400); subtitleVisible = true
    }

    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(surface, background),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Corner suit symbols
        val cornerSuits = listOf("♠", "♥", "♦", "♣")
        val cornerColors = listOf(onBackground, CardRed, CardRed, onBackground)
        val cornerAlignments = listOf(
            Alignment.TopStart, Alignment.TopEnd,
            Alignment.BottomEnd, Alignment.BottomStart
        )
        cornerSuits.forEachIndexed { i, suit ->
            AnimatedVisibility(
                visible = suitVisible[i].value,
                modifier = Modifier
                    .align(cornerAlignments[i])
                    .padding(40.dp),
                enter = scaleIn(
                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
                    initialScale = 0f
                ) + fadeIn(tween(300))
            ) {
                Text(suit, fontSize = 52.sp, color = cornerColors[i].copy(alpha = 0.12f))
            }
        }

        // Center content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 100.dp)
        ) {
            // Suits row — alpha + scale in graphicsLayer to avoid recomposition
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf("♠" to onBackground, "♥" to CardRed, "♦" to CardRed, "♣" to onBackground)
                    .forEachIndexed { i, (s, c) ->
                        val symAlpha by animateFloatAsState(
                            targetValue = if (suitVisible[i].value) 0.75f else 0f,
                            animationSpec = tween(300),
                            label = "symAlpha$i"
                        )
                        val symScale by animateFloatAsState(
                            targetValue = if (suitVisible[i].value) 1f else 0.4f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "symScale$i"
                        )
                        Text(
                            s,
                            fontSize = 32.sp,
                            color = c,
                            modifier = Modifier.graphicsLayer {
                                scaleX = symScale; scaleY = symScale; alpha = symAlpha
                            }
                        )
                    }
            }

            Spacer(Modifier.height(4.dp))

            // Shimmer title — isolated so infinite animation doesn't recompose parent
            AnimatedVisibility(
                visible = titleVisible,
                enter = slideInVertically(
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                ) { it / 4 } + fadeIn(tween(500))
            ) {
                ShimmerTitle()
            }

            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(tween(600))
            ) {
                Text(
                    "THE ULTIMATE CARD GAME",
                    fontSize = 13.sp,
                    letterSpacing = 3.sp,
                    color = onBackground.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = subtitleVisible,
                enter = fadeIn(tween(500)) + expandVertically(tween(500))
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = onBackground.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, onBackground.copy(alpha = 0.1f))
                ) {
                    Text(
                        "4–8 players  ·  2 teams  ·  48 cards\nAsk, strategise, and claim half suits",
                        fontSize = 14.sp,
                        color = onBackground.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
                    )
                }
            }
        }

    }
}

// ─── Page 2: The Deck ───────────────────────────────────────────────────────

private data class HalfSuitInfo(
    val suit: String,
    val name: String,
    val range: String,
    val darkColor: Color,   // 800–900 shades for dark theme
    val lightColor: Color,  // 500–700 shades for light theme
)

private val halfSuits = listOf(
    //                                    dark          light
    HalfSuitInfo("♠", "Low Spades",  "2  3  4  5  6  7",  Color(0xFF37474F), Color(0xFF607D8B)),
    HalfSuitInfo("♠", "High Spades", "9  10  J  Q  K  A", Color(0xFF263238), Color(0xFF546E7A)),
    HalfSuitInfo("♥", "Low Hearts",  "2  3  4  5  6  7",  Color(0xFFB71C1C), Color(0xFFE53935)),
    HalfSuitInfo("♥", "High Hearts", "9  10  J  Q  K  A", Color(0xFF880E4F), Color(0xFFC2185B)),
    HalfSuitInfo("♦", "Low Diamonds",  "2  3  4  5  6  7",  Color(0xFF1565C0), Color(0xFF1E88E5)),
    HalfSuitInfo("♦", "High Diamonds", "9  10  J  Q  K  A", Color(0xFF0D47A1), Color(0xFF1976D2)),
    HalfSuitInfo("♣", "Low Clubs",  "2  3  4  5  6  7",  Color(0xFF2E7D32), Color(0xFF43A047)),
    HalfSuitInfo("♣", "High Clubs", "9  10  J  Q  K  A", Color(0xFF1B5E20), Color(0xFF388E3C)),
)

@Composable
private fun DeckPage(isActive: Boolean) {
    val tileVisible = remember { Array(8) { mutableStateOf(false) } }
    var headerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (!isActive) {
            headerVisible = false
            repeat(8) { tileVisible[it].value = false }
            return@LaunchedEffect
        }
        delay(100); headerVisible = true
        repeat(8) { i -> delay(100L); tileVisible[i].value = true }
    }

    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(visible = headerVisible, enter = fadeIn(tween(400)) + slideInVertically { -it }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("The Deck", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
                Text(
                    "48 cards — split into 8 Half Suits of 6 cards each",
                    fontSize = 13.sp,
                    color = onBackground.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CardRed.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, CardRed.copy(alpha = 0.5f))
                ) {
                    Text(
                        "  ✕  No 8s in Literature!  ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CardRed,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 2, 4, 6).forEach { i ->
                    HalfSuitTile(visible = tileVisible[i].value, info = halfSuits[i])
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 5, 7).forEach { i ->
                    HalfSuitTile(visible = tileVisible[i].value, info = halfSuits[i])
                }
            }
        }
    }
}

@Composable
private fun HalfSuitTile(visible: Boolean, info: HalfSuitInfo) {
    val rotation by animateFloatAsState(
        targetValue = if (visible) 0f else 90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "flip"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "alpha"
    )
    val tileColor = if (isSystemInDarkTheme()) info.darkColor else info.lightColor
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .graphicsLayer { rotationY = rotation; cameraDistance = 14f * density; this.alpha = alpha }
            .clip(RoundedCornerShape(10.dp))
            .background(tileColor)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (rotation < 45f) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(info.suit, fontSize = 26.sp, color = Color.White.copy(alpha = 0.9f))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(info.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(info.range, fontSize = 10.sp, color = Color.White.copy(alpha = 0.65f))
                }
            }
        }
    }
}

// ─── Page 3: Teams ──────────────────────────────────────────────────────────

@Composable
private fun TeamsPage(isActive: Boolean) {
    var teamsVisible by remember { mutableStateOf(false) }
    val playerVisible = remember { Array(6) { mutableStateOf(false) } }
    var tipVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (!isActive) {
            teamsVisible = false
            repeat(6) { playerVisible[it].value = false }
            tipVisible = false
            return@LaunchedEffect
        }
        delay(150); teamsVisible = true
        delay(500)
        repeat(6) { i -> delay(130L); playerVisible[i].value = true }
        delay(300); tipVisible = true
    }

    val teamAOffset by animateFloatAsState(
        targetValue = if (teamsVisible) 0f else -1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "teamA"
    )
    val teamBOffset by animateFloatAsState(
        targetValue = if (teamsVisible) 0f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "teamB"
    )

    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 56.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Two Teams", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
            Text(
                "Collaborate with your teammates to claim the most half suits",
                fontSize = 13.sp,
                color = onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TeamColumn(
                teamName = "Team A",
                teamColor = LightGreen,
                players = listOf("You", "Alice", "Bob"),
                playerVisible = playerVisible.slice(0..2).map { it.value },
                onBackground = onBackground,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationX = teamAOffset * size.width
                        alpha = (1f + teamAOffset).coerceIn(0f, 1f)
                    }
            )
            TeamColumn(
                teamName = "Team B",
                teamColor = CardRed,
                players = listOf("Charlie", "Diana", "Eve"),
                playerVisible = playerVisible.slice(3..5).map { it.value },
                onBackground = onBackground,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        translationX = teamBOffset * size.width
                        alpha = (1f - teamBOffset).coerceIn(0f, 1f)
                    }
            )
        }

        AnimatedVisibility(
            visible = tipVisible,
            enter = fadeIn(tween(500)) + expandVertically(tween(400))
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GoldAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.2f))
            ) {
                Text(
                    "💡  Your asks reveal info to your whole team — think strategically before asking!",
                    fontSize = 13.sp,
                    color = onBackground.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun TeamColumn(
    teamName: String,
    teamColor: Color,
    players: List<String>,
    playerVisible: List<Boolean>,
    onBackground: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = teamColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, teamColor.copy(alpha = 0.4f))
        ) {
            Text(
                teamName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = teamColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        players.forEachIndexed { i, name ->
            AnimatedVisibility(
                visible = playerVisible[i],
                enter = fadeIn(tween(250)) + slideInVertically(tween(300)) { -it / 2 }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(teamColor.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(teamColor.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name.first().toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = teamColor)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(name, fontSize = 13.sp, color = onBackground.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ─── Page 4: The Ask ────────────────────────────────────────────────────────

@Composable
private fun AskPage() {
    // phase: 0=idle, 1=asking→opponent, 2=success card back, 3=pause,
    //        4=second ask→opponent, 5=fail bounce, 6=reset pause
    var phase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            phase = 0; delay(700)
            phase = 1; delay(750)
            phase = 2; delay(1000)
            phase = 3; delay(500)
            phase = 4; delay(750)
            phase = 5; delay(1100)
            phase = 6; delay(600)
        }
    }

    // Card Y: 0f = at You (bottom), -1f = at Opponent (top)
    val cardFraction by animateFloatAsState(
        targetValue = when (phase) {
            1, 4 -> -1f
            2    ->  0f
            5    -> -0.35f
            else ->  0f
        },
        animationSpec = when (phase) {
            1, 4 -> tween(650, easing = FastOutSlowInEasing)
            2    -> tween(600, easing = FastOutSlowInEasing)
            5    -> spring(dampingRatio = 0.25f, stiffness = 600f)
            else -> snap()
        },
        label = "cardY"
    )

    val cardColor by animateColorAsState(
        targetValue = when (phase) {
            2    -> LightGreen
            5    -> CardRed
            else -> GoldAccent
        },
        animationSpec = tween(300),
        label = "cardColor"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (phase == 2 || phase == 5) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    val onBackground = MaterialTheme.colorScheme.onBackground

    val labelText = when (phase) {
        1    -> "Asking for ♠7..."
        2    -> "✓  Got it! You can ask again"
        3, 4 -> "Asking for ♠6..."
        5    -> "✗  Denied — turn passes"
        else -> "Your turn to ask"
    }
    val labelColor = when (phase) {
        2    -> LightGreen
        5    -> CardRed
        else -> onBackground.copy(alpha = 0.75f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("The Ask", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
            Text(
                "On your turn, ask an opponent for a card — you must already hold one from that half suit",
                fontSize = 13.sp,
                color = onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        // Animation area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(onBackground.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            val travelRange = maxHeight.value * 0.32f

            // Opponent bubble (top)
            PlayerBubble(
                label = "Opponent",
                initial = "C",
                color = CardRed,
                onBackground = onBackground,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp)
            )

            // You bubble (bottom)
            PlayerBubble(
                label = "You",
                initial = "Y",
                color = LightGreen,
                onBackground = onBackground,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
            )

            // Animated card token — offset + scale in graphicsLayer to avoid recomposition
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        translationY = -cardFraction * travelRange
                        scaleX = cardScale; scaleY = cardScale
                    }
                    .size(36.dp, 48.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .drawBehind { drawRect(cardColor) },
                contentAlignment = Alignment.Center
            ) {
                val cardLabel = when (phase) {
                    1, 2, 3 -> "♠7"
                    4, 5, 6 -> "♠6"
                    else    -> "♠"
                }
                Text(
                    cardLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
        }

        // Animated label
        AnimatedContent(
            targetState = labelText,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "label"
        ) { text ->
            Text(
                text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                textAlign = TextAlign.Center
            )
        }

        // Rules
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RuleRow(icon = "✓", text = "Success → take the card and ask again", color = LightGreen, onBackground = onBackground)
            RuleRow(icon = "✗", text = "Denied → turn passes to that opponent", color = CardRed, onBackground = onBackground)
            RuleRow(icon = "⚠", text = "You must hold a card from that half suit", color = GoldAccent, onBackground = onBackground)
        }
    }
}

@Composable
private fun PlayerBubble(label: String, initial: String, color: Color, onBackground: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
        Text(label, fontSize = 11.sp, color = onBackground.copy(alpha = 0.5f))
    }
}

@Composable
private fun RuleRow(icon: String, text: String, color: Color, onBackground: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 13.sp, color = onBackground.copy(alpha = 0.7f))
    }
}

// ─── Page 5: Claim & Win ────────────────────────────────────────────────────

@Composable
private fun ClaimBadge(visible: Boolean) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
        ) + fadeIn()
    ) {
        Surface(shape = RoundedCornerShape(8.dp), color = GoldAccent) {
            Text(
                "  ♠ Low Spades — CLAIMED!  ",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ClaimPage(onFinish: () -> Unit, isActive: Boolean) {
    var cardsGathered by remember { mutableStateOf(false) }
    var badgeVisible by remember { mutableStateOf(false) }
    var showScore by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var buttonVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (!isActive) {
            cardsGathered = false
            badgeVisible = false
            showScore = false
            score = 0
            buttonVisible = false
            return@LaunchedEffect
        }
        delay(350); cardsGathered = true
        delay(750); badgeVisible = true
        delay(400); showScore = true
        repeat(5) { delay(220L); score++ }
        delay(300); buttonVisible = true
    }

    val cardProgress by animateFloatAsState(
        targetValue = if (cardsGathered) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 180f),
        label = "cardProgress"
    )

    // Pulsing button scale
    val buttonScale by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 1f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "buttonScale"
        )

    // Scattered card starting offsets (x, y in dp)
    val scatterOffsets = remember {
        listOf(
            Offset(-72f, -44f), Offset(68f, -62f), Offset(-88f, 8f),
            Offset(82f, 16f),   Offset(-56f, 54f), Offset(60f, 62f)
        )
    }
    val cardValues = listOf("A", "2", "3", "4", "5", "6")

    val onBackground = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Claim & Win", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = GoldAccent)
            Text(
                "When your team holds all 6 cards of a half suit — claim it for a point!",
                fontSize = 13.sp,
                color = onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        // Card gather animation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Use derivedStateOf for color threshold — emits only when crossing 0.85
            val cardsReady by remember { derivedStateOf { cardProgress > 0.85f } }
            scatterOffsets.forEachIndexed { i, offset ->
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val progress = 1f - cardProgress
                            translationX = offset.x * progress * density
                            translationY = offset.y * progress * density
                            rotationZ = offset.x * 0.12f * progress
                            alpha = 0.6f + 0.4f * cardProgress
                        }
                        .size(34.dp, 46.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (cardsReady) FeltGreen.copy(alpha = 0.9f)
                            else Color(0xFFEEEEEE)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        cardValues[i],
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (cardsReady) GoldAccent else Color(0xFF222222)
                    )
                }
            }

            // "CLAIMED!" badge snaps in below the cards
            Box(modifier = Modifier.offset(y = 60.dp)) {
                ClaimBadge(visible = badgeVisible)
            }
        }

        // Score ticker
        AnimatedVisibility(
            visible = showScore,
            enter = fadeIn(tween(400)) + expandVertically(tween(400))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your Team", fontSize = 13.sp, color = onBackground.copy(alpha = 0.45f))
                Text(
                    "$score",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LightGreen,
                    lineHeight = 70.sp
                )
                Text("half suits claimed", fontSize = 13.sp, color = onBackground.copy(alpha = 0.45f))
                Spacer(Modifier.height(4.dp))
                Text(
                    "Most half suits wins!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldAccent
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Let's Play! button
        AnimatedVisibility(
            visible = buttonVisible,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
        ) {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .navigationBarsPadding()
                    .width(240.dp)
                    .height(56.dp)
                    .graphicsLayer { scaleX = buttonScale; scaleY = buttonScale },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "Let's Play!  ♠♥♦♣",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
        }
    }
}
