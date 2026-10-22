package com.cards.game.literature.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The name limit used to be a plain `take(20)`, which counts UTF-16 units and can stop halfway
 * through an emoji: the other players then saw a broken glyph where the last character was.
 */
class PlayerNameTest {

    @Test
    fun aShortNameIsOnlyTrimmed() {
        assertEquals("Vinu", "  Vinu ".asPlayerName())
    }

    @Test
    fun aLongNameIsCutAtTheLimit() {
        assertEquals("ABCDEFGHIJKLMNOPQRST", "ABCDEFGHIJKLMNOPQRSTUVWXYZ".asPlayerName())
    }

    @Test
    fun anEmojiOnTheBoundaryIsDroppedWholeNotHalved() {
        // 19 letters + 🃏 (two UTF-16 units): unit 20 is the first half of the emoji.
        val name = "ABCDEFGHIJKLMNOPQRS\uD83C\uDCCF".asPlayerName()
        assertEquals("ABCDEFGHIJKLMNOPQRS", name)
        assertFalse(name.any { it.isSurrogate() }, "no lone surrogate may survive truncation")
    }

    @Test
    fun anEmojiThatFitsIsKept() {
        // 18 letters + 🃏 = exactly 20 units.
        val name = "ABCDEFGHIJKLMNOPQR\uD83C\uDCCF"
        assertEquals(name, name.asPlayerName())
    }
}
