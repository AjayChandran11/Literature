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
        AnalyticsEvent.GameStarted(mode = "online", teamSize = 3, hasBots = true, turnTimerSecs = null),
        AnalyticsEvent.GameFinished(mode = "offline_bots", won = false, teamSize = 2, durationSecs = 420L),
        AnalyticsEvent.DailyPuzzleOpened,
        AnalyticsEvent.DailyPuzzleSolved(kind = "wasted_ask", stars = 3, firstTry = true, streak = 7),
        AnalyticsEvent.InviteShared(surface = "waiting_room"),
        AnalyticsEvent.InviteOpened,
        AnalyticsEvent.AchievementUnlocked(id = "ROOKIE_DETECTIVE"),
    )

    @Test
    fun eventNamesAreStable() {
        assertEquals("game_started", AnalyticsEvent.GameStarted("online", 3, true, null).name)
        assertEquals("game_finished", AnalyticsEvent.GameFinished("online", true, 3, null).name)
        assertEquals("daily_puzzle_opened", AnalyticsEvent.DailyPuzzleOpened.name)
        assertEquals("daily_puzzle_solved", AnalyticsEvent.DailyPuzzleSolved("claim", 3, true, 1).name)
        assertEquals("invite_shared", AnalyticsEvent.InviteShared("waiting_room").name)
        assertEquals("invite_opened", AnalyticsEvent.InviteOpened.name)
        assertEquals("achievement_unlocked", AnalyticsEvent.AchievementUnlocked("X").name)
    }

    @Test
    fun paramsCarryTheExpectedKeysAndValues() {
        val started = AnalyticsEvent.GameStarted(mode = "online", teamSize = 3, hasBots = true, turnTimerSecs = null)
        assertEquals(mapOf("mode" to "online", "team_size" to 3, "has_bots" to true), started.params)

        val finished = AnalyticsEvent.GameFinished(mode = "online", won = true, teamSize = 3, durationSecs = 120L)
        assertEquals(
            mapOf("mode" to "online", "won" to true, "team_size" to 3, "duration_secs" to 120L),
            finished.params,
        )

        val solved = AnalyticsEvent.DailyPuzzleSolved(kind = "locate", stars = 2, firstTry = false, streak = 4)
        assertEquals(
            mapOf("kind" to "locate", "stars" to 2, "first_try" to false, "streak" to 4),
            solved.params,
        )

        assertEquals(mapOf("surface" to "waiting_room"), AnalyticsEvent.InviteShared("waiting_room").params)
        assertEquals(mapOf("achievement_id" to "CASE_MASTER"), AnalyticsEvent.AchievementUnlocked("CASE_MASTER").params)
    }

    @Test
    fun optionalParamsAreOmittedWhenNull() {
        // turn_timer_secs / duration_secs must not appear as keys when their source value is null.
        assertTrue("turn_timer_secs" !in AnalyticsEvent.GameStarted("online", 3, false, null).params)
        assertTrue("duration_secs" !in AnalyticsEvent.GameFinished("online", true, 3, null).params)
        // ...and present when supplied.
        assertTrue("turn_timer_secs" in AnalyticsEvent.GameStarted("online", 3, false, 30).params)
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
