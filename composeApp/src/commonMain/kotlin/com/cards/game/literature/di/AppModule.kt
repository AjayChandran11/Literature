package com.cards.game.literature.di

import com.cards.game.literature.bot.BotPlayer
import com.cards.game.literature.bot.BotStrategy
import com.cards.game.literature.logic.CardTracker
import com.cards.game.literature.logic.GameEngine
import com.cards.game.literature.repository.GameRepository
import com.cards.game.literature.repository.LocalGameRepository
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.viewmodel.GameViewModel
import com.cards.game.literature.viewmodel.LobbyViewModel
import com.cards.game.literature.viewmodel.ResultViewModel
import com.cards.game.literature.viewmodel.WaitingRoomViewModel
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single { GameEngine() }
    single { CardTracker() }
    single { BotStrategy(get()) }
    single { BotPlayer(get()) }
    single<GameRepository> { LocalGameRepository(get(), get()) }

    // Online dependencies
    single {
        HttpClient {
            install(WebSockets)
        }
    }
    single { OnlineGameRepository(serverUrl = serverUrl, client = get()) }

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
}
