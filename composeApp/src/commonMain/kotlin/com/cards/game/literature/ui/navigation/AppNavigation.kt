package com.cards.game.literature.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cards.game.literature.bot.BotDifficulty
import com.cards.game.literature.deeplink.DeepLinkHandler
import com.cards.game.literature.stats.PuzzleStore
import com.cards.game.literature.stats.currentEpochDay
import com.cards.game.literature.repository.OnlineGameRepository
import com.cards.game.literature.ui.game.GameBoardScreen
import com.cards.game.literature.ui.home.HomeScreen
import com.cards.game.literature.ui.lobby.LobbyScreen
import com.cards.game.literature.ui.lobby.WaitingRoomScreen
import com.cards.game.literature.preferences.OnboardingPrefs
import com.cards.game.literature.ui.onboarding.OnboardingScreen
import com.cards.game.literature.ui.dailypuzzle.DailyPuzzleScreen
import com.cards.game.literature.ui.result.ResultScreen
import com.cards.game.literature.ui.stats.StatsScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val GAME = "game/{playerName}/{playerCount}/{difficulty}"
    const val ONLINE_GAME = "online_game"
    const val RESULT = "result"
    const val RESULT_ONLINE = "result_online"
    const val LOBBY = "lobby/{playerName}?room={roomCode}"
    const val WAITING_ROOM = "waiting_room/{roomCode}"
    const val STATS = "stats"
    const val DAILY_PUZZLE = "daily_puzzle"

    fun game(playerName: String, playerCount: Int, difficulty: BotDifficulty = BotDifficulty.MEDIUM) = "game/$playerName/$playerCount/${difficulty.name}"
    fun lobby(playerName: String) = "lobby/$playerName"
    // Deep-link invite: lands in the lobby with the room code prefilled to auto-join.
    fun lobby(playerName: String, roomCode: String) = "lobby/$playerName?room=$roomCode"
    fun waitingRoom(roomCode: String) = "waiting_room/$roomCode"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val startDestination = if (OnboardingPrefs.isCompleted()) Routes.HOME else Routes.ONBOARDING

    // Jump straight to a screen the app was launched into (e.g. the daily-puzzle reminder tap).
    val pendingDestination by DeepLinkHandler.pendingDestination.collectAsState()
    LaunchedEffect(pendingDestination) {
        when (pendingDestination) {
            DeepLinkHandler.LaunchDestination.DAILY_PUZZLE -> {
                // Only if past onboarding — otherwise the puzzle would stack on the onboarding flow.
                if (OnboardingPrefs.isCompleted()) {
                    // The reminder already nudged the player and we're taking them to the puzzle,
                    // so mark the day's ready-hint as shown — no need to badge Home on the way back.
                    PuzzleStore.markReadyHintShown(currentEpochDay())
                    navController.navigate(Routes.DAILY_PUZZLE) { launchSingleTop = true }
                }
                DeepLinkHandler.consumeDestination()
            }
            null -> {}
        }
    }

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
                onStartGame = { playerName, playerCount, difficulty ->
                    navController.navigate(Routes.game(playerName, playerCount, difficulty))
                },
                onPlayOnline = { playerName ->
                    navController.navigate(Routes.lobby(playerName))
                },
                onJoinRoom = { playerName, roomCode ->
                    navController.navigate(Routes.lobby(playerName, roomCode))
                },
                onOpenStats = {
                    navController.navigate(Routes.STATS)
                },
                onOpenDailyPuzzle = {
                    navController.navigate(Routes.DAILY_PUZZLE)
                }
            )
        }
        composable(Routes.STATS) {
            StatsScreen(
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
        composable(Routes.DAILY_PUZZLE) {
            DailyPuzzleScreen(
                onBack = { navController.popBackStack(Routes.HOME, inclusive = false) }
            )
        }
        composable(Routes.GAME) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Player"
            val playerCount = backStackEntry.arguments?.getString("playerCount")?.toIntOrNull() ?: 6
            val difficulty = backStackEntry.arguments?.getString("difficulty")
                ?.let { runCatching { BotDifficulty.valueOf(it) }.getOrNull() }
                ?: BotDifficulty.MEDIUM
            GameBoardScreen(
                playerName = playerName,
                playerCount = playerCount,
                difficulty = difficulty,
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
        composable(
            Routes.LOBBY,
            arguments = listOf(
                navArgument("roomCode") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName") ?: "Player"
            val initialRoomCode = backStackEntry.arguments?.getString("roomCode")
            LobbyScreen(
                playerName = playerName,
                initialRoomCode = initialRoomCode,
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
                },
                onRematchNavigate = { roomCode ->
                    navController.navigate(Routes.waitingRoom(roomCode)) {
                        popUpTo(Routes.HOME)
                    }
                }
            )
        }
    }
}
