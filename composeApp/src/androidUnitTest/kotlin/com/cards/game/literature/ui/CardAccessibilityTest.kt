package com.cards.game.literature.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import com.cards.game.literature.model.Card
import com.cards.game.literature.model.CardValue
import com.cards.game.literature.model.Suit
import com.cards.game.literature.ui.game.CardView
import com.cards.game.literature.ui.theme.LiteratureTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What a screen reader is told about a playing card.
 *
 * The audit found the hand unusable with TalkBack: cards carried `onClick = {}`, so every one of
 * them promised an activation that did nothing, and a selected card sounded exactly like an
 * unselected one. These assertions run against a real composition, so they hold the semantics
 * rather than the implementation that happens to produce them.
 */
@RunWith(RobolectricTestRunner::class)
// A stub Application, not the app's own: LiteratureApplication starts Koin in onCreate,
// which collides with the DI tests running in the same JVM. Nothing here needs the graph.
@Config(sdk = [34], application = android.app.Application::class)
class CardAccessibilityTest {

    private val fiveOfHearts = Card(Suit.HEARTS, CardValue.FIVE)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aCardIsAnnouncedByItsRankAndSuit() = runComposeUiTest {
        setContent { LiteratureTheme { CardView(card = fiveOfHearts, isSelected = false) } }

        // "5 of Hearts" — not "card", and not the bare glyph.
        onNodeWithContentDescription("5 of Hearts").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aCardYouCannotTapDoesNotClaimToBeTappable() = runComposeUiTest {
        // This is the hand: cards are shown, not chosen. The no-op handler that used to sit here
        // made TalkBack offer "double tap to activate" on all of them.
        setContent { LiteratureTheme { CardView(card = fiveOfHearts, isSelected = false) } }

        val matches = onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue(matches.isEmpty(), "a display-only card must expose no click action")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aSelectableCardOffersSelectAndReportsItsState() = runComposeUiTest {
        setContent {
            LiteratureTheme { CardView(card = fiveOfHearts, isSelected = false, onClick = {}) }
        }

        val node = onNodeWithContentDescription("5 of Hearts").fetchSemanticsNode()
        assertEquals(false, node.config.getOrNull(SemanticsProperties.Selected), "reports unselected")
        assertEquals(
            "Select 5 of Hearts",
            node.config.getOrNull(SemanticsActions.OnClick)?.label,
            "and says what tapping does"
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aSelectedCardSaysSoAndOffersToRemoveIt() = runComposeUiTest {
        setContent {
            LiteratureTheme { CardView(card = fiveOfHearts, isSelected = true, onClick = {}) }
        }

        val node = onNodeWithContentDescription("5 of Hearts").fetchSemanticsNode()
        assertEquals(true, node.config.getOrNull(SemanticsProperties.Selected), "reports selected")
        assertEquals(
            "Remove 5 of Hearts from selection",
            node.config.getOrNull(SemanticsActions.OnClick)?.label,
            "and the action flips with the state"
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun tappingASelectableCardCallsBack() = runComposeUiTest {
        var taps = 0
        setContent {
            LiteratureTheme { CardView(card = fiveOfHearts, isSelected = false, onClick = { taps++ }) }
        }

        onNodeWithContentDescription("5 of Hearts").performClick()

        assertEquals(1, taps)
    }
}
