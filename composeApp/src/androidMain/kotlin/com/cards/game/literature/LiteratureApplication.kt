package com.cards.game.literature

import android.app.Application
import com.cards.game.literature.di.appModule
import com.cards.game.literature.notifications.AppLifecycleObserver
import com.cards.game.literature.notifications.NotificationCoordinator
import com.cards.game.literature.notifications.Notifier
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
    }
}
