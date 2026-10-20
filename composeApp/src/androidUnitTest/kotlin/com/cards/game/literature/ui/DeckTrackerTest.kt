package com.cards.game.literature.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runComposeUiTest
import com.cards.game.literature.model.HalfSuit
import com.cards.game.literature.model.HalfSuitStatus
import com.cards.game.literature.ui.game.DeckTracker
import com.cards.game.literature.ui.theme.LiteratureTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The portrait half-suit tracker on a 360 dp phone. Eight cells never fit, so the row scrolls;
 * the contract is that the row always says which way the rest of the sets are. Half of them
 * used to sit off-screen with nothing to show it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w360dp-h640dp-xhdpi")
class DeckTrackerTest {

    private val statuses = HalfSuit.entries.map { HalfSuitStatus(it) }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun atRestTheRowHintsAtMoreToTheRight() = runComposeUiTest {
        setContent { LiteratureTheme { DeckTracker(statuses = statuses, myTeamId = "team_1") } }

        onNodeWithTag("deck_tracker_fade_end").assertExists()
        onNodeWithTag("deck_tracker_fade_start").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrolledToTheEndTheHintSwitchesSides() = runComposeUiTest {
        setContent { LiteratureTheme { DeckTracker(statuses = statuses, myTeamId = "team_1") } }

        onNode(hasScrollAction()).performScrollToIndex(statuses.lastIndex)

        onNodeWithTag("deck_tracker_fade_end").assertDoesNotExist()
        onNodeWithTag("deck_tracker_fade_start").assertExists()
    }
}
