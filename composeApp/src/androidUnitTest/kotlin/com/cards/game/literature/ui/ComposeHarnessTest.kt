package com.cards.game.literature.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves the Compose UI-test harness itself works before anything is built on it.
 *
 * These run on the JVM through Robolectric, so they need no device and no emulator and can sit
 * in CI beside the ordinary unit tests. If this one fails, nothing else in this source set is
 * telling the truth.
 */
@RunWith(RobolectricTestRunner::class)
// A stub Application, not the app's own: LiteratureApplication starts Koin in onCreate,
// which collides with the DI tests running in the same JVM. Nothing here needs the graph.
@Config(sdk = [34], application = android.app.Application::class)
class ComposeHarnessTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theHarnessCanRenderAndFindAComposable() = runComposeUiTest {
        setContent { Text("dealt") }

        onNodeWithText("dealt").assertIsDisplayed()
    }
}
