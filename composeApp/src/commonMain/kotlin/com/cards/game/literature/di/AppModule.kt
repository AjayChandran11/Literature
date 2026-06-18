package com.cards.game.literature.di

import com.cards.game.literature.preferences.SessionStore
import com.cards.game.literature.logic.GameEngine
import com.cards.game.literature.notifications.NotificationCoordinator
import com.cards.game.literature.repository.GameRepository
import com.cards.game.literature.repository.LocalGameRepository
import com.cards.game.literature.repository.DailyPuzzleRepository
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.viewmodel.DailyPuzzleViewModel
import com.cards.game.literature.viewmodel.GameViewModel
import com.cards.game.literature.viewmodel.LobbyViewModel
import com.cards.game.literature.viewmodel.ResultViewModel
import com.cards.game.literature.viewmodel.WaitingRoomViewModel
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { SessionStore() }
    single { GameEngine() }
    single<GameRepository> { LocalGameRepository(get()) }

    // Online dependencies
    single {
        HttpClient {
            install(WebSockets)
            install(HttpTimeout)
        }
    }
    single { OnlineGameRepository(serverUrl = serverUrl, client = get()) }
    single { NotificationCoordinator(get()) }

    viewModel { GameViewModel(get()) }
    viewModel(qualifier = named("online")) {
        val onlineRepo = get<OnlineGameRepository>()
        GameViewModel(onlineRepo, overridePlayerId = onlineRepo.myPlayerId)
    }
    viewModel { ResultViewModel(get()) }
    viewModel(qualifier = named("online")) {
        val onlineRepo = get<OnlineGameRepository>()
        ResultViewModel(onlineRepo, onlineRepo.myPlayerId)
    }
    viewModel { LobbyViewModel(get()) }
    viewModel { WaitingRoomViewModel(get()) }

    single { DailyPuzzleRepository() }
    viewModel { DailyPuzzleViewModel(get()) }
}
