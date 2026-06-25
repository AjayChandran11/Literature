package com.cards.game.literature

import android.app.Application
import com.cards.game.literature.di.appModule
import com.cards.game.literature.notifications.AppLifecycleObserver
import com.cards.game.literature.notifications.NotificationCoordinator
import com.cards.game.literature.notifications.Notifier
import com.cards.game.literature.notifications.PuzzleReminderScheduler
import com.cards.game.literature.preferences.GamePrefs
import com.cards.game.literature.preferences.StatsPrefs
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class LiteratureApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(appModule)
        }

        Notifier.init(this)
        AppLifecycleObserver.init()
        GlobalContext.get().get<NotificationCoordinator>().start()

        // Prefs are read from the alarm's broadcast process (where the Activity never runs), so init
        // them here in the Application — the one entry point common to every process start.
        GamePrefs.init(this)
        StatsPrefs.init(this)
        PuzzleReminderScheduler.init(this)
        if (GamePrefs.isPuzzleReminderEnabled()) PuzzleReminderScheduler.schedule()
    }
}
