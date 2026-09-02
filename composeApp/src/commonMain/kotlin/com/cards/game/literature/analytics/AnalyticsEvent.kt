package com.cards.game.literature.analytics

/**
 * Type-safe catalogue of the product-analytics events we report. Modelling each event as a
 * subclass (rather than raw name + bundle calls scattered across call sites) keeps every event
 * name and parameter key defined in exactly one place, so the Firebase console stays consistent
 * and a typo can't silently fork one event into two.
 *
 * Naming follows Firebase's rules: snake_case, <= 40 chars, starts with a letter, and avoids the
 * reserved names / prefixes (firebase_, google_, ga_). Param string values should stay <= 100 chars.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
) {
    /** A match began. [mode] is one of "online", "offline_bots". */
    class GameStarted(
        mode: String,
        teamSize: Int,
        hasBots: Boolean,
        turnTimerSecs: Int?,
    ) : AnalyticsEvent(
        name = "game_started",
        params = buildMap {
            put("mode", mode)
            put("team_size", teamSize)
            put("has_bots", hasBots)
            if (turnTimerSecs != null) put("turn_timer_secs", turnTimerSecs)
        },
    )

    /** A match ended. [mode] mirrors [GameStarted]. */
    class GameFinished(
        mode: String,
        won: Boolean,
        teamSize: Int,
        durationSecs: Long?,
    ) : AnalyticsEvent(
        name = "game_finished",
        params = buildMap {
            put("mode", mode)
            put("won", won)
            put("team_size", teamSize)
            if (durationSecs != null) put("duration_secs", durationSecs)
        },
    )

    /** Player left the intro carousel. [completed] = reached the end; false = tapped Skip.
     *  Top of the first-time funnel — pairs with [TutorialCompleted] and [GameStarted]. */
    class OnboardingFinished(completed: Boolean) : AnalyticsEvent(
        name = "onboarding_finished",
        params = mapOf("completed" to completed),
    )

    /** The first-game coached tutorial ran to completion. */
    data object TutorialCompleted : AnalyticsEvent(name = "tutorial_completed")

    /** Player shared the end-of-game result card (a growth surface distinct from a room invite). */
    data object ResultShared : AnalyticsEvent(name = "result_shared")

    /** Player opened the Daily Puzzle screen. */
    data object DailyPuzzleOpened : AnalyticsEvent(name = "daily_puzzle_opened")

    /** Player solved today's puzzle. [kind] mirrors PuzzleKind (e.g. "claim", "locate", "wasted_ask"). */
    class DailyPuzzleSolved(
        kind: String,
        stars: Int,
        firstTry: Boolean,
        streak: Int,
    ) : AnalyticsEvent(
        name = "daily_puzzle_solved",
        params = buildMap {
            put("kind", kind)
            put("stars", stars)
            put("first_try", firstTry)
            put("streak", streak)
        },
    )

    /** Player opened the online lobby — the step above [RoomCreated] in the room funnel. Also
     *  fires on a deep-link arrival, so subtract [InviteOpened] to count players who chose
     *  online themselves. */
    data object LobbyOpened : AnalyticsEvent(name = "lobby_opened")

    /** Player asked to create an online room. Fires on the attempt, not on admission — the drop
     *  to [GameStarted] with mode "online" covers both creates that never landed (a cold server)
     *  and rooms that landed but never filled. */
    class RoomCreated(playerCount: Int) : AnalyticsEvent(
        name = "room_created",
        params = mapOf("player_count" to playerCount),
    )

    /** Player shared a room-invite link — top of the Phase 2 invite funnel. [surface] e.g.
     *  "waiting_room"; [channel] is "whatsapp" (direct) or "system" (the OS share sheet), so we
     *  can measure the WhatsApp-first lift. */
    class InviteShared(surface: String, channel: String = "system") : AnalyticsEvent(
        name = "invite_shared",
        params = mapOf("surface" to surface, "channel" to channel),
    )

    /** App opened via a room-invite deep link — bottom of the Phase 2 invite funnel. */
    data object InviteOpened : AnalyticsEvent(name = "invite_opened")

    /** A fresh install that came through an invite link auto-surfaced its room on first
     *  launch, read from the Play Install Referrer (join.html → Play → install → open). */
    data object InstallReferrerJoin : AnalyticsEvent(name = "install_referrer_join")

    /** An achievement was unlocked (gameplay or puzzle). */
    class AchievementUnlocked(id: String) : AnalyticsEvent(
        name = "achievement_unlocked",
        params = mapOf("achievement_id" to id),
    )
}
