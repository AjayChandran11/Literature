package com.cards.game.literature

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cards.game.literature.di.appModule
import org.koin.compose.KoinApplication
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.cards.game.literature.audio.SoundPlayer
import com.cards.game.literature.deeplink.DeepLinkHandler
import com.cards.game.literature.network.NetworkMonitor
import com.cards.game.literature.notifications.Notifier
import com.cards.game.literature.preferences.GamePrefs
import com.cards.game.literature.preferences.OnboardingPrefs
import com.cards.game.literature.preferences.StatsPrefs
import com.cards.game.literature.preferences.TutorialPrefs
import com.cards.game.literature.share.Sharer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialise singletons BEFORE setContent so composables can read them
        OnboardingPrefs.init(this)
        TutorialPrefs.init(this)
        NetworkMonitor.init(this)
        GamePrefs.init(this)
        StatsPrefs.init(this)
        SoundPlayer.init(this)
        Sharer.init(this)
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true

        // Handle a room invite or notification tap that launched the app (cold start).
        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    // Handle intents that arrive while the app is already running (singleTop reuses
    // this instance and delivers the intent here instead of a fresh onCreate).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // Room invite via App Link / custom scheme.
        if (intent.action == Intent.ACTION_VIEW) {
            DeepLinkHandler.submit(intent.dataString)
        }

        // Daily-puzzle reminder tap. Skip when relaunched from Recents (the extra lingers on
        // the task's root intent) so we only jump to the puzzle on the actual notification tap.
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (!fromHistory && intent.getStringExtra(Notifier.EXTRA_NAVIGATE_TO) == Notifier.NAV_DAILY_PUZZLE) {
            DeepLinkHandler.submitDestination(DeepLinkHandler.LaunchDestination.DAILY_PUZZLE)
            intent.removeExtra(Notifier.EXTRA_NAVIGATE_TO)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    KoinApplication(application = { modules(appModule) }) {
        App()
    }
}
