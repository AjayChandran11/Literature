package com.cards.game.literature.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cards.game.literature.model.GamePhase
import com.cards.game.literature.repository.ConnectionState
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.ui.game.GameBoardContent
import com.cards.game.literature.viewmodel.GameViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named

@Composable
fun OnlineGameScreen(
    onlineRepository: OnlineGameRepository,
    onGameEnd: () -> Unit,
    viewModel: GameViewModel = koinViewModel(qualifier = named("online"))
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by onlineRepository.connectionState.collectAsState()

    // Navigate to result when game ends
    LaunchedEffect(uiState.phase) {
        if (uiState.phase == GamePhase.FINISHED) {
            onGameEnd()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameBoardContent(viewModel = viewModel)

        // Connection status overlay
        AnimatedVisibility(
            visible = connectionState == ConnectionState.RECONNECTING || connectionState == ConnectionState.DISCONNECTED,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (connectionState == ConnectionState.RECONNECTING)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                        else
                            MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (connectionState == ConnectionState.RECONNECTING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Reconnecting...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    } else {
                        Text(
                            "Disconnected",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }
        }
    }
}
