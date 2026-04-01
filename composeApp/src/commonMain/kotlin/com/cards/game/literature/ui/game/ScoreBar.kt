package com.cards.game.literature.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

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
            Text(
                "$myTeamScore",
                style = MaterialTheme.typography.headlineLarge,
                color = LightGreen
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
            Text(
                "$opponentTeamScore",
                style = MaterialTheme.typography.headlineLarge,
                color = CardRed
            )
        }
    }
}
