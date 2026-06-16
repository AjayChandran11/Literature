package com.cards.game.literature.ui.result

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cards.game.literature.ui.theme.LiteratureTheme
import com.cards.game.literature.viewmodel.ResultUiState

// Previews for the shareable result card. The card is captured against the dark
// palette regardless of system theme, so preview it with darkTheme = true at the
// 4:5 aspect it ships in (~1080x1350 at 3x density).

private fun sample(
    my: Int,
    opp: Int,
    win: Boolean,
    draw: Boolean = false,
    myName: String = "Your Team",
    oppName: String = "Opponents"
) = ResultUiState(
    myTeamScore = my,
    opponentTeamScore = opp,
    myTeamName = myName,
    opponentTeamName = oppName,
    isWinner = win,
    isDraw = draw
)

@Preview(name = "Result card — Win", showBackground = true)
@Composable
private fun ResultShareCardWinPreview() {
    LiteratureTheme(darkTheme = true) {
        ResultShareCard(sample(5, 4, win = true), Modifier.size(360.dp, 450.dp))
    }
}

@Preview(name = "Result card — Lose", showBackground = true)
@Composable
private fun ResultShareCardLosePreview() {
    LiteratureTheme(darkTheme = true) {
        ResultShareCard(sample(3, 5, win = false), Modifier.size(360.dp, 450.dp))
    }
}

@Preview(name = "Result card — Draw", showBackground = true)
@Composable
private fun ResultShareCardDrawPreview() {
    LiteratureTheme(darkTheme = true) {
        ResultShareCard(sample(4, 4, win = false, draw = true), Modifier.size(360.dp, 450.dp))
    }
}

@Preview(name = "Result card — long team names", showBackground = true)
@Composable
private fun ResultShareCardLongNamesPreview() {
    LiteratureTheme(darkTheme = true) {
        ResultShareCard(
            sample(6, 2, win = true, myName = "The Magnificent Spades", oppName = "Hearts United FC"),
            Modifier.size(360.dp, 450.dp)
        )
    }
}
