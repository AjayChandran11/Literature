package com.cards.game.literature.bot

/**
 * Cosmetic flavour for a bot player: a stable emoji "face" tied to the bot's name.
 *
 * Bots are always named from a fixed pool (see [BotPersonalities.ALL], which
 * [com.cards.game.literature.logic.GameEngine.getBotName] and the server's
 * `GameRoom` both draw from), so the client can derive a bot's look purely from its
 * name — nothing extra has to travel over the wire. Personalities are presentation
 * only: every bot still plays at the difficulty the host selected.
 *
 * A bot whose name isn't in the pool — e.g. a disconnected human who is replaced
 * mid-game keeps their own name — falls back to [BotPersonalities.GENERIC_EMOJI],
 * which doubles as a visible "this seat is now a bot" cue.
 */
data class BotPersonality(val name: String, val emoji: String)

object BotPersonalities {
    /** Shown for any bot that has no dedicated personality (e.g. a replaced human). */
    const val GENERIC_EMOJI = "🤖" // 🤖

    /**
     * The bot roster. The names here MUST match the names assigned in
     * [com.cards.game.literature.logic.GameEngine.getBotName] and the server's
     * `GameRoom` bot list, or those bots fall back to [GENERIC_EMOJI].
     */
    val ALL: List<BotPersonality> = listOf(
        BotPersonality("Alice", "🦊"),   // 🦊 sly fox
        BotPersonality("Bob", "🐢"),      // 🐢 slow and steady
        BotPersonality("Charlie", "🦉"),  // 🦉 owl, forgets nothing
        BotPersonality("Diana", "🐱"),    // 🐱 cat
        BotPersonality("Eve", "🦅"),      // 🦅 sharp-eyed eagle
        BotPersonality("Frank", "🐻"),    // 🐻 bear
        BotPersonality("Grace", "🦋"),    // 🦋 butterfly
    )

    private val byName: Map<String, BotPersonality> = ALL.associateBy { it.name }

    /** The dedicated personality for [name], or null if the name isn't in the roster. */
    fun forName(name: String): BotPersonality? = byName[name]

    /** Emoji for a bot named [name]; the generic robot face if it has no personality. */
    fun emojiFor(name: String): String = byName[name]?.emoji ?: GENERIC_EMOJI
}
