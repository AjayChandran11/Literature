package com.cards.game.literature.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.GoldAccent
import com.cards.game.literature.viewmodel.ResultUiState
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.label_opponents
import literature.composeapp.generated.resources.label_your_team
import literature.composeapp.generated.resources.result_card_cta
import literature.composeapp.generated.resources.result_card_tagline
import literature.composeapp.generated.resources.result_draw
import literature.composeapp.generated.resources.result_lose
import literature.composeapp.generated.resources.result_win
import org.jetbrains.compose.resources.stringResource

// Brand palette (from the Play Store poster): midnight-navy field, gold wordmark.
private val NavyCenter = Color(0xFF26264A)
private val NavyEdge = Color(0xFF111120)
private val InkWhite = Color(0xFFF3F3F7)
private val DimWhite = Color(0xFFB9BBD0)

/**
 * Branded, self-contained end-of-game card rendered to an image for social sharing.
 * Matches the Play Store poster (navy + gold "Literature" + suits + tagline) and leads
 * with the player's result as the hook. No scroll / buttons / animations — captures as a
 * single stable frame. Laid out at a fixed square size (~360.dp → 1080px at 3x density).
 */
@Composable
fun ResultShareCard(uiState: ResultUiState, modifier: Modifier = Modifier) {
    val outcomeText = when {
        uiState.isDraw -> stringResource(Res.string.result_draw)
        uiState.isWinner -> stringResource(Res.string.result_win)
        else -> stringResource(Res.string.result_lose)
    }
    val outcomeColor = when {
        uiState.isWinner -> GoldAccent
        uiState.isDraw -> InkWhite
        else -> CardRed
    }
    val emoji = when {
        uiState.isWinner -> "🏆 "
        uiState.isDraw -> "🤝 "
        else -> ""
    }
    val myName = uiState.myTeamName.ifBlank { stringResource(Res.string.label_your_team) }
    val oppName = uiState.opponentTeamName.ifBlank { stringResource(Res.string.label_opponents) }

    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(NavyCenter, NavyEdge),
                radius = 760f
            )
        )
    ) {
        // Faded corner suits — subtle brand texture (mirrors the Home screen).
        CornerSuit("♠", Alignment.TopStart)
        CornerSuit("♥", Alignment.TopEnd, CardRed)
        CornerSuit("♦", Alignment.BottomStart, CardRed)
        CornerSuit("♣", Alignment.BottomEnd)

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Brand header ──
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Literature",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldAccent
                )
                Spacer(Modifier.height(10.dp))
                SuitRow()
            }

            // ── Result hero ──
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$emoji$outcomeText",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = outcomeColor,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ScorePill(myName, uiState.myTeamScore, GoldAccent.takeIf { uiState.isWinner } ?: InkWhite, Modifier.weight(1f))
                    Text("–", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = DimWhite)
                    ScorePill(oppName, uiState.opponentTeamScore, InkWhite, Modifier.weight(1f))
                }
            }

            // ── Footer: tagline + CTA ──
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.result_card_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.result_card_cta),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(GoldAccent.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SuitRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("♠", fontSize = 22.sp, color = InkWhite)
        Text("♥", fontSize = 22.sp, color = CardRed)
        Text("♦", fontSize = 22.sp, color = CardRed)
        Text("♣", fontSize = 22.sp, color = InkWhite)
    }
}

@Composable
private fun ScorePill(name: String, score: Int, scoreColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$score", fontSize = 60.sp, fontWeight = FontWeight.ExtraBold, color = scoreColor)
        Spacer(Modifier.height(2.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = DimWhite,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BoxScope.CornerSuit(symbol: String, alignment: Alignment, color: Color = InkWhite) {
    Text(
        text = symbol,
        fontSize = 60.sp,
        color = color.copy(alpha = 0.06f),
        modifier = Modifier.align(alignment).padding(10.dp)
    )
}
