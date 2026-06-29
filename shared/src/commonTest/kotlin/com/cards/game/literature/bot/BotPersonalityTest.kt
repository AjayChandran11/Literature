package com.cards.game.literature.bot

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BotPersonalityTest {

    @Test
    fun everyRosterNameResolvesToItsOwnPersonality() {
        for (p in BotPersonalities.ALL) {
            assertEquals(p, BotPersonalities.forName(p.name), "forName should round-trip ${p.name}")
            assertEquals(p.emoji, BotPersonalities.emojiFor(p.name), "emojiFor should match roster for ${p.name}")
        }
    }

    @Test
    fun unknownNameFallsBackToGenericEmoji() {
        // A replaced human keeps their own name, which isn't in the roster.
        assertNull(BotPersonalities.forName("Ravi"))
        assertEquals(BotPersonalities.GENERIC_EMOJI, BotPersonalities.emojiFor("Ravi"))
        assertEquals(BotPersonalities.GENERIC_EMOJI, BotPersonalities.emojiFor("Bot 9"))
    }

    @Test
    fun rosterNamesAreUniqueAndNonBlank() {
        val names = BotPersonalities.ALL.map { it.name }
        assertEquals(names.size, names.toSet().size, "bot names must be unique")
        assertTrue(names.all { it.isNotBlank() }, "bot names must be non-blank")
    }

    @Test
    fun rosterEmojisAreUniqueAndDistinctFromGeneric() {
        val emojis = BotPersonalities.ALL.map { it.emoji }
        assertEquals(emojis.size, emojis.toSet().size, "bot emojis must be unique so bots are distinguishable")
        for (e in emojis) {
            assertNotEquals(BotPersonalities.GENERIC_EMOJI, e, "a personality emoji must not collide with the generic fallback")
        }
    }

    /**
     * Tripwire: the roster names must stay in lock-step with the names assigned by
     * [com.cards.game.literature.logic.GameEngine] (offline) and the server's `GameRoom`
     * (online). If this list changes, update both name sources too — otherwise those bots
     * silently fall back to the generic emoji.
     */
    @Test
    fun rosterMatchesTheCanonicalBotNames() {
        assertEquals(
            listOf("Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace"),
            BotPersonalities.ALL.map { it.name }
        )
    }
}
