package com.cards.game.literature.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.ui.game.GameBoardScreen
import com.cards.game.literature.ui.home.HomeScreen
import com.cards.game.literature.ui.lobby.LobbyScreen
import com.cards.game.literature.ui.lobby.WaitingRoomScreen
import com.cards.game.literature.preferences.OnboardingPrefs
import com.cards.game.literature.ui.onboarding.OnboardingScreen
import com.cards.game.literature.ui.result.ResultScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val GAME = "game/{playerName}/{playerCount}"
    const val ONLINE_GAME = "online_game"
    const val RESULT = "result"
    const val RESULT_ONLINE = "result_online"
    const val LOBBY = "lobby/{playerName}"
    const val WAITING_ROOM = "waiting_room/{roomCode}"

    fun game(playerName: String, playerCount: Int) = "game/$playerName/$playerCount"
    fun lobby(playerName: String) = "lobby/$playerName"
    fun waitingRoom(roomCode: String) = "waiting_room/$roomCode"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val startDestination = if (OnboardingPrefs.isCompleted()) Routes.HOME else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    OnboardingPrefs.markCompleted()
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onStartGame = { playerName, playerCount ->
                    navController.navigate(Routes.game(playerName, playerCount))
                },
                onPlayOnline = { playerName ->
                    navController.navigate(Routes.lobby(playerName))
                }
            )
        }
        composable(Routes.GAME) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Player"
            val playerCount = backStackEntry.arguments?.getString("playerCount")?.toIntOrNull() ?: 6
            GameBoardScreen(
                playerName = playerName,
                playerCount = playerCount,
                onGameEnd = {
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.HOME)
                    }
                },
                onQuit = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
        composable(Routes.LOBBY) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Player"
            LobbyScreen(
                playerName = playerName,
                onNavigateToWaitingRoom = { roomCode ->
                    navController.navigate(Routes.waitingRoom(roomCode)) {
                        popUpTo(Routes.lobby(playerName)) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
        composable(Routes.WAITING_ROOM) { backStackEntry ->
            val roomCode = backStackEntry.arguments?.getString("roomCode") ?: ""
            WaitingRoomScreen(
                onGameStart = {
                    navController.navigate(Routes.ONLINE_GAME) {
                        popUpTo(Routes.HOME)
                    }
                },
                onLeave = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
        composable(Routes.ONLINE_GAME) {
            val onlineRepo = koinInject<OnlineGameRepository>()
            OnlineGameScreen(
                onlineRepository = onlineRepo,
                onGameEnd = {
                    navController.navigate(Routes.RESULT_ONLINE) {
                        popUpTo(Routes.HOME)
                    }
                },
                onQuit = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                onPlayAgain = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onGoHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
        composable(Routes.RESULT_ONLINE) {
            ResultScreen(
                viewModel = koinViewModel(qualifier = named("online")),
                onPlayAgain = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onGoHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
