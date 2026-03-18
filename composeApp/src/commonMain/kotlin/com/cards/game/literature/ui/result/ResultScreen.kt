package com.cards.game.literature.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.GameEvent
import com.cards.game.literature.ui.game.GameLogEntry
import com.cards.game.literature.ui.theme.CardRed
import com.cards.game.literature.ui.theme.LightGreen
import com.cards.game.literature.viewmodel.ResultViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ResultScreen(
    onPlayAgain: () -> Unit,
    onGoHome: () -> Unit,
    viewModel: ResultViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Winner banner
        Text(
            text = when {
                uiState.isDraw -> "It's a Draw!"
                uiState.isWinner -> "You Win!"
                else -> "You Lose!"
            },
            style = MaterialTheme.typography.displaySmall,
            color = when {
                uiState.isDraw -> MaterialTheme.colorScheme.secondary
                uiState.isWinner -> LightGreen
                else -> CardRed
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.myTeamName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${uiState.myTeamScore}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = LightGreen
                )
            }
            Text(
                "-",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(uiState.opponentTeamName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${uiState.opponentTeamScore}",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = CardRed
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Breakdown
        Text(
            "Half-Suit Breakdown",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            uiState.halfSuitBreakdown.forEach { status ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        status.halfSuit.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val claimedBy = when (status.claimedByTeamId) {
                        "team_1" -> uiState.myTeamName
                        "team_2" -> uiState.opponentTeamName
                        else -> "Unclaimed"
                    }
                    val claimColor = when (status.claimedByTeamId) {
                        "team_1" -> LightGreen
                        "team_2" -> CardRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        claimedBy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = claimColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Game Log
        OutlinedButton(
            onClick = { showLog = !showLog },
            modifier = Modifier.fillMaxWidth(0.7f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(if (showLog) "Hide Game Log" else "Show Game Log")
        }

        if (showLog) {
            Spacer(modifier = Modifier.height(8.dp))
            val displayEvents = uiState.gameLog.filterNot {
                it is GameEvent.TurnChanged || it is GameEvent.GameStarted
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                items(displayEvents) { event ->
                    GameLogEntry(event = event, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Play Again", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Home")
        }
    }
}
