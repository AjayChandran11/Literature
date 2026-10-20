package com.cards.game.literature.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.cards.game.literature.model.currentTimeMillis
import com.cards.game.literature.ui.game.TurnTimerText
import com.cards.game.literature.ui.theme.LiteratureTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The turn countdown's contract, now that three places draw it: the header, the ask sheet and
 * the claim sheet. It shows only inside the last 15 seconds, shows nothing when no clock runs,
 * and freezes while paused. A regression in any of those would be invisible in a unit test of
 * the surrounding screens — they only see "some text" or none.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TurnCountdownTest {

    @androidx.compose.runtime.Composable
    private fun Countdown(deadlineMs: Long?, paused: Boolean = false) {
        LiteratureTheme {
            TurnTimerText(
                deadlineMs = deadlineMs,
                timerPaused = paused,
                style = MaterialTheme.typography.titleMedium,
                normalColor = Color.Gray,
                spacer = 4.dp,
            )
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun insideTheLastFifteenSecondsTheCountdownIsShown() = runComposeUiTest {
        setContent { Countdown(deadlineMs = currentTimeMillis() + 8_000) }

        // 8s, or 7s if the test clock straddled a second boundary.
        onNode(hasText("8s") or hasText("7s")).assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun withNoClockRunningNothingIsShown() = runComposeUiTest {
        setContent { Countdown(deadlineMs = null) }

        onNodeWithText("s", substring = true).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aboveFifteenSecondsNothingIsShownYet() = runComposeUiTest {
        // Most of a turn is quiet; the countdown is a warning, not a stopwatch.
        setContent { Countdown(deadlineMs = currentTimeMillis() + 40_000) }

        onNodeWithText("s", substring = true).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun whilePausedTheCountdownDoesNotRun() = runComposeUiTest {
        // The tutorial pauses it: a first-timer reading the walkthrough must not be rushed.
        setContent { Countdown(deadlineMs = currentTimeMillis() + 8_000, paused = true) }

        onNodeWithText("s", substring = true).assertDoesNotExist()
    }
}
