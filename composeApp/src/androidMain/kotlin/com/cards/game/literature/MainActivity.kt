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

        // Handle a room invite that launched the app (cold start).
        handleDeepLink(intent)

        enableEdgeToEdge()
        setContent {
            App()
        }
    }

    // Handle invites that arrive while the app is already running (singleTop reuses
    // this instance and delivers the intent here instead of a fresh onCreate).
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        DeepLinkHandler.submit(intent.dataString)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    KoinApplication(application = { modules(appModule) }) {
        App()
    }
}
