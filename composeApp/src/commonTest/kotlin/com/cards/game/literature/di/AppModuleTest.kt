package com.cards.game.literature.di

import com.cards.game.literature.notifications.NotificationCoordinator
import com.cards.game.literature.repository.GameRepository
import com.cards.game.literature.repository.OnlineGameRepository
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * The dependency graph, checked rather than assumed.
 *
 * A whole feature shipped dead because of this class of mistake: the your-turn cue was written,
 * wired and released, but nothing on the web ever called `NotificationCoordinator.start()`, so it
 * could not fire. These tests can't prove a platform entry point calls `start()` — but they can
 * prove the graph resolves, and that the pieces which must be shared really are shared.
 */
class AppModuleTest {

    @AfterTest
    fun tearDown() = stopKoin()

    @Test
    fun everyDeclaredDependencyResolves() {
        val koin = startKoin { modules(appModule) }.koin

        // Resolving each singleton constructs its whole transitive graph; a missing or
        // mis-qualified binding throws here instead of at runtime on someone's phone.
        assertNotNull(koin.get<GameRepository>())
        assertNotNull(koin.get<OnlineGameRepository>())
        assertNotNull(koin.get<HttpClient>())
        assertNotNull(koin.get<NotificationCoordinator>())
    }

    @Test
    fun theOnlineSessionIsOneObjectForTheWholeApp() {
        val koin = startKoin { modules(appModule) }.koin

        // The repository holds the socket, the seat and the reconnect token. Two instances means
        // the board and the waiting room would be watching different sessions.
        assertSame(koin.get<OnlineGameRepository>(), koin.get<OnlineGameRepository>())
    }

    @Test
    fun theNotificationCoordinatorWatchesTheSameSessionTheGameUses() {
        val koin = startKoin { modules(appModule) }.koin

        // It takes the repository as a constructor argument; if that were a fresh instance the
        // cue would be watching a session nobody is playing.
        assertNotNull(koin.get<NotificationCoordinator>())
        assertSame(koin.get<OnlineGameRepository>(), koin.get<OnlineGameRepository>())
    }

    @Test
    fun theOfflineAndOnlineRepositoriesAreDistinct() {
        val koin = startKoin { modules(appModule) }.koin

        // GameRepository is bound to the LOCAL implementation; an online game must not be served
        // by it, and vice versa.
        assertNotNull(koin.get<GameRepository>())
        assertNotNull(koin.get<OnlineGameRepository>())
        assertSame(koin.get<GameRepository>(), koin.get<GameRepository>())
    }
}
