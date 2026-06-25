package com.cards.game.literature

import androidx.compose.ui.window.ComposeUIViewController
import com.cards.game.literature.notifications.AppLifecycleObserver
import com.cards.game.literature.notifications.PuzzleReminderScheduler
import com.cards.game.literature.preferences.GamePrefs

fun MainViewController() = ComposeUIViewController {
    AppLifecycleObserver.init()
    if (GamePrefs.isPuzzleReminderEnabled()) PuzzleReminderScheduler.schedule()
    App()
}
