package com.cards.game.literature

import android.app.Application
import com.cards.game.literature.di.appModule
import org.koin.core.context.startKoin

class LiteratureApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            modules(appModule)
        }
    }
}
