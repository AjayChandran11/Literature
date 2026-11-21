package com.cards.game.literature.ui.dailypuzzle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The shape of the shared result. It is read in a WhatsApp group by people who have not played
 * yet, so it must say how it went without saying what the answer was, and it must carry the link
 * that lets them play the same puzzle in the browser.
 */
class PuzzleShareTextTest {

    private val link = "https://literature-game.pages.dev/?puzzle=126"

    @Test
    fun aSolveIsStarsStreakKindAndLink() {
        val text = PuzzleShareText.format(puzzleNumber = 126, solved = true, stars = 3, streak = 7, kindTitle = "The wasted ask", link = link)
        assertEquals("Literature #126 ⭐⭐⭐ 🔥7\nThe wasted ask\n$link", text)
    }

    @Test
    fun noStreakMeansNoFlame() {
        val text = PuzzleShareText.format(puzzleNumber = 126, solved = true, stars = 2, streak = 0, kindTitle = "Locate the card", link = link)
        assertEquals("Literature #126 ⭐⭐\nLocate the card\n$link", text)
    }

    @Test
    fun aFailedDayHasACrossAndNoStars() {
        val text = PuzzleShareText.format(puzzleNumber = 126, solved = false, stars = 0, streak = 0, kindTitle = "The claim", link = link)
        assertEquals("Literature #126 ✗\nThe claim\n$link", text)
        assertFalse("⭐" in text)
    }

    @Test
    fun theLinkIsAlwaysTheLastLine() {
        assertEquals(link, PuzzleShareText.format(126, true, 3, 0, "The claim", link).lines().last())
    }
}
