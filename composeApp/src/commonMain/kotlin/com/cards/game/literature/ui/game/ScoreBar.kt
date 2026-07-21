package com.cards.game.literature.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * A score number that rolls to its new value: the old digit slides up and out
 * while the new one slides in from below (reversed when the score decreases,
 * which only happens on a game reset). Scores move by claims — a rare, earned
 * moment — so the tick draws the eye without needing to be loud.
 */
@Composable
internal fun AnimatedScoreText(
    score: Int,
    color: Color,
    style: TextStyle,
    fontWeight: FontWeight? = null
) {
    AnimatedContent(
        targetState = score,
        transitionSpec = {
            if (targetState >= initialState) {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            } else {
                (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
            }
        }
    ) { value ->
        Text("$value", style = style, color = color, fontWeight = fontWeight)
    }
}

@Composable
fun ScoreBar(myTeamScore: Int, opponentTeamScore: Int, modifier: Modifier = Modifier) {
    val scoreDesc = stringResource(Res.string.cd_score, myTeamScore, opponentTeamScore)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = scoreDesc }
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(Res.string.label_your_team), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedScoreText(
                score = myTeamScore,
                color = LightGreen,
                style = MaterialTheme.typography.headlineLarge
            )
        }
        Text(
            stringResource(Res.string.score_vs),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(Res.string.label_opponents), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedScoreText(
                score = opponentTeamScore,
                color = CardRed,
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}
