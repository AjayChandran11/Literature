# Literature — Card Game

A fully playable **Literature** card game for Android, built with Kotlin Multiplatform + Compose Multiplatform. Play against bots in 4, 6, or 8-player modes with full game rules enforcement. This game is currently in development and it is getting created using Claude. 

---

## What is Literature?

Literature is a strategic team card game played with a 48-card deck (standard 52-card deck with all four 8s removed). Cards are split into 8 **half-suits** of 6 cards each:

| Half-Suit | Cards |
|-----------|-------|
| ♠ Lower | 2♠ 3♠ 4♠ 5♠ 6♠ 7♠ |
| ♠ Upper | 9♠ 10♠ J♠ Q♠ K♠ A♠ |
| ♥ Lower | 2♥ 3♥ 4♥ 5♥ 6♥ 7♥ |
| ♥ Upper | 9♥ 10♥ J♥ Q♥ K♥ A♥ |
| ♦ Lower | 2♦ 3♦ 4♦ 5♦ 6♦ 7♦ |
| ♦ Upper | 9♦ 10♦ J♦ Q♦ K♦ A♦ |
| ♣ Lower | 2♣ 3♣ 4♣ 5♣ 6♣ 7♣ |
| ♣ Upper | 9♣ 10♣ J♣ Q♣ K♣ A♣ |

Teams compete to **claim all 8 half-suits** by correctly declaring which player on their team holds each of the 6 cards.

### How to Play

1. **Ask** an opponent for a card — you must already hold a card from the same half-suit
2. If they have it, the card transfers to you and **you keep your turn**
3. If they don't, the **turn passes to them**
4. **Claim** a half-suit on your turn by declaring the exact location of all 6 cards across your teammates
5. Correct claim → **+1 point** for your team. Wrong claim → **+1 point** for opponents
6. First team to win **5 of 8 half-suits** wins (4–4 is a draw)

---

## App Features

- **Single-player vs bots** — you + teammates vs bot opponents (4, 6, or 8-player modes)
- **Full rules enforcement** — move validation, turn management, card tracking, player elimination
- **Level 1 Bot AI** — bots track the game log to infer card locations and make intelligent ask/claim decisions with a realistic 1.5–2.5s thinking delay
- **Multi-step ask flow** — pick half-suit → pick card → pick opponent → confirm
- **Multi-step claim flow** — pick half-suit → assign all 6 cards to teammates → confirm
- **Deck tracker** — 8 tiles showing unclaimed (yellow), your team (green), opponent (red) half-suits
- **Game log** — collapsible live event feed with color coding
- **Dark green felt theme** with gold accents

---

## Tech Stack

| Concern | Technology |
|---------|-----------|
| UI | Compose Multiplatform 1.10.0 |
| Language | Kotlin 2.3.0 |
| Shared Logic | Kotlin Multiplatform |
| DI | Koin 4.1.0 |
| Navigation | AndroidX Navigation Compose 2.9.2 |
| Serialization | kotlinx.serialization 1.8.1 |
| Async | Kotlin Coroutines 1.10.2 + Flow |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

---

## Build & Run

```bash
# Build debug APK
./gradlew :composeApp:assembleDebug

# Run unit tests
./gradlew :composeApp:testDebugUnitTest

# iOS simulator framework
./gradlew :composeApp:iosSimulatorArm64Binaries
```

APK output: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`

---

## Future: Multiplayer

The architecture is designed for a server upgrade with minimal changes:

1. Add `server/` Ktor module (WebSocket handler, room management, `GameEngine` reuse)
2. Create `RemoteGameRepository` implementing the same `GameRepository` interface
3. Add Lobby screen (room code, team assignment)
4. Swap Koin binding — **zero UI changes needed**

See `literature game.md` for the full multiplayer API spec (WebSocket messages, room management, session handling).

---
