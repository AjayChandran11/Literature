package com.cards.game.literature.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks the analytics event contract: names + parameter keys stay stable (so historical data in the
 * Firebase console doesn't fork on a rename), and every event obeys Firebase's naming rules. Runs on
 * plain Kotlin — no Firebase dependency — because [AnalyticsEvent] is pure data.
 */
class AnalyticsEventTest {

    /** One representative instance of every event, so the rule checks cover the whole catalogue. */
    private val sampleEvents: List<AnalyticsEvent> = listOf(
        AnalyticsEvent.GameStarted(mode = "online", playerCount = 6, hasBots = true, isHost = false),
        AnalyticsEvent.GameFinished(mode = "offline_bots", won = false, result = "draw", playerCount = 4, durationSecs = 420L),
        AnalyticsEvent.TutorialSkipped(step = "half_suits"),
        AnalyticsEvent.OnboardingFinished(completed = true),
        AnalyticsEvent.TutorialCompleted,
        AnalyticsEvent.ResultShared,
        AnalyticsEvent.DailyPuzzleOpened,
        AnalyticsEvent.DailyPuzzleSolved(kind = "wasted_ask", stars = 3, firstTry = true, streak = 7),
        AnalyticsEvent.LobbyOpened,
        AnalyticsEvent.RoomCreated(playerCount = 6),
        AnalyticsEvent.RoomJoined(source = "invite", playerCount = 6),
        AnalyticsEvent.InviteShared(surface = "waiting_room"),
        AnalyticsEvent.InviteOpened(source = "app_link"),
        AnalyticsEvent.InstallReferrerJoin,
        AnalyticsEvent.AchievementUnlocked(id = "ROOKIE_DETECTIVE"),
        AnalyticsEvent.GameLeft(mode = "online"),
        AnalyticsEvent.RematchRequested(mode = "offline_bots"),
        AnalyticsEvent.ReviewRequested(trigger = "game"),
        AnalyticsEvent.DailyPuzzleShared(solved = true, channel = "whatsapp"),
        AnalyticsEvent.PuzzleLinkOpened(puzzleNumber = 126),
        AnalyticsEvent.AppShared,
    )

    @Test
    fun eventNamesAreStable() {
        assertEquals("game_started", AnalyticsEvent.GameStarted("online", 3, true).name)
        assertEquals("game_finished", AnalyticsEvent.GameFinished("online", true, "win", 3, null).name)
        assertEquals("tutorial_skipped", AnalyticsEvent.TutorialSkipped("players").name)
        assertEquals("daily_puzzle_opened", AnalyticsEvent.DailyPuzzleOpened.name)
        assertEquals("daily_puzzle_solved", AnalyticsEvent.DailyPuzzleSolved("claim", 3, true, 1).name)
        assertEquals("lobby_opened", AnalyticsEvent.LobbyOpened.name)
        assertEquals("room_created", AnalyticsEvent.RoomCreated(6).name)
        assertEquals("room_joined", AnalyticsEvent.RoomJoined("code", 4).name)
        assertEquals("invite_shared", AnalyticsEvent.InviteShared("waiting_room").name)
        assertEquals("invite_opened", AnalyticsEvent.InviteOpened("web").name)
        assertEquals("install_referrer_join", AnalyticsEvent.InstallReferrerJoin.name)
        assertEquals("achievement_unlocked", AnalyticsEvent.AchievementUnlocked("X").name)
        assertEquals("onboarding_finished", AnalyticsEvent.OnboardingFinished(true).name)
        assertEquals("tutorial_completed", AnalyticsEvent.TutorialCompleted.name)
        assertEquals("result_shared", AnalyticsEvent.ResultShared.name)
        assertEquals("game_left", AnalyticsEvent.GameLeft("online").name)
        assertEquals("rematch_requested", AnalyticsEvent.RematchRequested("online").name)
        assertEquals("review_requested", AnalyticsEvent.ReviewRequested("game").name)
        assertEquals("daily_puzzle_shared", AnalyticsEvent.DailyPuzzleShared(true, "system").name)
        assertEquals("daily_puzzle_link_opened", AnalyticsEvent.PuzzleLinkOpened(1).name)
        assertEquals("app_shared", AnalyticsEvent.AppShared.name)
    }

    @Test
    fun paramsCarryTheExpectedKeysAndValues() {
        val started = AnalyticsEvent.GameStarted(mode = "online", playerCount = 6, hasBots = true, isHost = true)
        assertEquals(mapOf("mode" to "online", "player_count" to 6, "has_bots" to true, "is_host" to true), started.params)
        val startedOffline = AnalyticsEvent.GameStarted(mode = "offline_bots", playerCount = 4, hasBots = true)
        assertEquals(mapOf("mode" to "offline_bots", "player_count" to 4, "has_bots" to true), startedOffline.params)

        val finished = AnalyticsEvent.GameFinished(mode = "online", won = true, result = "win", playerCount = 6, durationSecs = 120L)
        assertEquals(
            mapOf("mode" to "online", "won" to true, "result" to "win", "player_count" to 6, "duration_secs" to 120L),
            finished.params,
        )

        val solved = AnalyticsEvent.DailyPuzzleSolved(kind = "locate", stars = 2, firstTry = false, streak = 4)
        assertEquals(
            mapOf("kind" to "locate", "stars" to 2, "first_try" to false, "streak" to 4),
            solved.params,
        )

        assertEquals(mapOf("player_count" to 6), AnalyticsEvent.RoomCreated(6).params)
        assertEquals(mapOf("source" to "invite", "player_count" to 6), AnalyticsEvent.RoomJoined("invite", 6).params)
        assertEquals(mapOf("source" to "referrer"), AnalyticsEvent.InviteOpened("referrer").params)

        assertEquals(
            mapOf("surface" to "waiting_room", "channel" to "system"),
            AnalyticsEvent.InviteShared("waiting_room").params,
        )
        assertEquals(
            mapOf("surface" to "waiting_room", "channel" to "whatsapp"),
            AnalyticsEvent.InviteShared("waiting_room", channel = "whatsapp").params,
        )
        assertEquals(mapOf("achievement_id" to "CASE_MASTER"), AnalyticsEvent.AchievementUnlocked("CASE_MASTER").params)
        assertEquals(mapOf("completed" to true), AnalyticsEvent.OnboardingFinished(true).params)
        assertEquals(mapOf("completed" to false), AnalyticsEvent.OnboardingFinished(false).params)

        assertEquals(mapOf("mode" to "online"), AnalyticsEvent.GameLeft("online").params)
        assertEquals(mapOf("mode" to "offline_bots"), AnalyticsEvent.RematchRequested("offline_bots").params)
        assertEquals(mapOf("trigger" to "puzzle"), AnalyticsEvent.ReviewRequested("puzzle").params)
        assertEquals(mapOf("solved" to false, "channel" to "copy"), AnalyticsEvent.DailyPuzzleShared(false, "copy").params)
        assertEquals(mapOf("puzzle_number" to 126), AnalyticsEvent.PuzzleLinkOpened(126).params)
        assertTrue(AnalyticsEvent.AppShared.params.isEmpty())
        // A draw used to be reported as won=false, indistinguishable from a loss.
        assertEquals("draw", AnalyticsEvent.GameFinished("online", false, "draw", 4, null).params["result"])
        assertEquals(mapOf("step" to "hand_tab"), AnalyticsEvent.TutorialSkipped("hand_tab").params)
    }

    @Test
    fun playerCountIsOneDimensionAcrossRoomAndGameEvents() {
        // room_created said player_count while game_started/finished said team_size for the same
        // thing, so no single GA4 dimension could follow a game from room to result.
        val events = listOf(
            AnalyticsEvent.RoomCreated(6),
            AnalyticsEvent.RoomJoined("code", 6),
            AnalyticsEvent.GameStarted("online", 6, false),
            AnalyticsEvent.GameFinished("online", true, "win", 6, null),
        )
        for (event in events) assertEquals(6, event.params["player_count"], "player_count on ${event.name}")
        assertTrue(sampleEvents.none { "team_size" in it.params }, "team_size must be gone")
    }

    @Test
    fun optionalParamsAreOmittedWhenNull() {
        // is_host / duration_secs must not appear as keys when their source value is null.
        assertTrue("is_host" !in AnalyticsEvent.GameStarted("online", 3, false, null).params)
        assertTrue("duration_secs" !in AnalyticsEvent.GameFinished("online", true, "win", 3, null).params)
        // ...and present when supplied.
        assertTrue("is_host" in AnalyticsEvent.GameStarted("online", 3, false, isHost = false).params)
    }

    @Test
    fun everyEventObeysFirebaseNamingRules() {
        // Firebase: names/param keys start with a letter, are alphanumeric + underscore, <= 40 chars,
        // and must not use the reserved prefixes.
        val token = Regex("^[a-zA-Z][a-zA-Z0-9_]*$")
        val reservedPrefixes = listOf("firebase_", "google_", "ga_")
        for (event in sampleEvents) {
            assertTrue(token.matches(event.name), "bad event name: ${event.name}")
            assertTrue(event.name.length <= 40, "event name too long: ${event.name}")
            assertTrue(
                reservedPrefixes.none { event.name.startsWith(it) },
                "event name uses reserved prefix: ${event.name}",
            )
            for (key in event.params.keys) {
                assertTrue(token.matches(key), "bad param key: $key on ${event.name}")
                assertTrue(key.length <= 40, "param key too long: $key on ${event.name}")
                assertTrue(
                    reservedPrefixes.none { key.startsWith(it) },
                    "param key uses reserved prefix: $key on ${event.name}",
                )
            }
        }
    }
}
