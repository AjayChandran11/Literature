package com.cards.game.literature

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.cards.game.literature.audio.SoundPlayer
import com.cards.game.literature.network.NetworkMonitor
import com.cards.game.literature.preferences.GamePrefs
import com.cards.game.literature.preferences.OnboardingPrefs
import com.cards.game.literature.preferences.TutorialPrefs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialise singletons BEFORE setContent so composables can read them
        OnboardingPrefs.init(this)
        TutorialPrefs.init(this)
        NetworkMonitor.init(this)
        GamePrefs.init(this)
        SoundPlayer.init(this)
        FirebaseApp.initializeApp(this)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true

        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
