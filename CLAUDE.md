# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Android
./gradlew :composeApp:assembleDebug        # Build debug APK
./gradlew :composeApp:assembleRelease      # Build release APK
./gradlew :composeApp:build                # Full build + tests
./gradlew :composeApp:clean                # Clean build outputs

# Shared module
./gradlew :shared:build                    # Build shared library (all targets)
./gradlew :shared:jvmTest                  # Run shared module tests

# Server
./gradlew :server:build                    # Build server
./gradlew :server:run                      # Run server locally (port 8080)

# Tests
./gradlew :composeApp:assembleUnitTest     # Assemble unit tests
./gradlew :composeApp:assembleAndroidTest  # Assemble instrumented tests

# iOS — compile Kotlin to framework (then open iosApp/iosApp.xcodeproj in Xcode)
./gradlew :composeApp:iosSimulatorArm64Binaries   # Simulator
./gradlew :composeApp:iosArm64Binaries            # Physical device
```

## Architecture

**Kotlin Multiplatform (KMP) + Compose Multiplatform** targeting Android and iOS from a single codebase, with a Ktor WebSocket server for multiplayer.

### Module Structure

```
Literature/
  shared/              # KMP library (Android + iOS + JVM targets)
    src/commonMain/    #   model/, logic/, bot/, protocol/
    src/androidMain/   #   actual fun currentTimeMillis()
    src/iosMain/       #   actual fun currentTimeMillis()
    src/jvmMain/       #   actual fun currentTimeMillis()
  composeApp/          # Client app (depends on :shared)
    src/commonMain/    #   ui/, viewmodel/, repository/, di/
  server/              # Ktor JVM server (depends on :shared)
    src/main/kotlin/   #   Application, RoomManager, GameRoom, GameWebSocket
```

### shared/ module
Contains all game logic shared between client and server:
- `model/` — Card, Player, Team, GameState, GameEvent, ClaimDeclaration, HalfSuit, etc.
- `logic/` — GameEngine, MoveValidator, ClaimEvaluator, CardDealer, DeckUtils, CardTracker
- `bot/` — BotPlayer, BotStrategy, BotAction
- `protocol/` — ClientMessage, ServerMessage, PlayerGameView, RoomState (WebSocket wire format)

### composeApp/ module
Client-side code:
- `ui/` — All Compose UI screens (home, lobby, game, result)
- `viewmodel/` — GameViewModel, ResultViewModel, LobbyViewModel, WaitingRoomViewModel
- `repository/` — GameRepository interface, LocalGameRepository (offline), OnlineGameRepository (WebSocket)
- `di/` — Koin dependency injection setup

### server/ module
Ktor WebSocket server for multiplayer:
- `Application.kt` — Server entry point (Netty, port 8080)
- `RoomManager` — Creates/joins/cleans up rooms, generates 6-char room codes
- `GameRoom` — Per-room game state, GameEngine integration, bot turns, disconnect handling
- `GameWebSocket` — Single `/game` WebSocket endpoint routing ClientMessages to rooms

**Platform abstraction pattern (`expect`/`actual`):**
- `shared/commonMain/model/GameEvent.kt` declares `expect fun currentTimeMillis(): Long`
- `actual` implementations in `androidMain/`, `iosMain/`, `jvmMain/`
- `composeApp/commonMain/Platform.kt` declares `expect fun getPlatform(): Platform`

**Entry points:**
- Android: `MainActivity.kt` → `setContent { App() }`
- iOS: `MainViewController.kt` → `ComposeUIViewController { App() }` (called from Swift in `iosApp/`)
- Server: `Application.kt` → `embeddedServer(Netty, port = 8080)`

## Key Versions

| Tool | Version |
|------|---------|
| Kotlin | 2.3.0 |
| Compose Multiplatform | 1.10.0 |
| AGP | 8.11.2 |
| Ktor | 3.1.3 |
| Min SDK | 24 |
| Target/Compile SDK | 36 |
| JVM target | 11 |

Version catalog: `gradle/libs.versions.toml`

## Multiplayer Protocol

Client connects via WebSocket to `ws://host:8080/game`. Messages are JSON-serialized sealed classes:
- **Client → Server:** CreateRoom, JoinRoom, StartGame, AskCards, ClaimDeck, LeaveRoom, Reconnect
- **Server → Client:** RoomCreated, RoomUpdate, GameStarted, GameUpdate, GameEventOccurred, Error, RoomClosed

Server holds full GameState; each client receives a `PlayerGameView` with only their own hand visible.

## Development Workflow

1. Start server: `./gradlew :server:run`
2. Run app on emulator: `./gradlew :composeApp:assembleDebug` (connects to `10.0.2.2:8080`)
3. For physical device: update `serverUrl` in `AppModule.kt` to your machine's local IP

## Known Issues

The current setup (KMP plugin + AGP in the same module) is deprecated and will break with AGP 9.0. The future fix is to split into a separate KMP library module and a standalone Android app module.
