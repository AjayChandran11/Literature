package com.cards.game.literature.ui.dailypuzzle

/**
 * The spoiler-free result for sharing today's puzzle: the stars and streak, the puzzle's kind,
 * and a link that opens the same puzzle in the browser with no install. It carries no card, seat
 * or half-suit, so it can be posted before friends have played. Three lines on purpose: the
 * stars already say how many tries it took, so nothing else is needed.
 */
internal object PuzzleShareText {
    fun format(
        puzzleNumber: Int,
        solved: Boolean,
        stars: Int,
        streak: Int,
        kindTitle: String,
        link: String,
    ): String {
        val header = buildString {
            append("Literature #").append(puzzleNumber)
            if (solved) {
                if (stars > 0) append(' ').append("⭐".repeat(stars))
            } else {
                append(" ✗")
            }
            if (streak > 0) append(" \uD83D\uDD25").append(streak) // 🔥
        }
        return listOf(header, kindTitle, link).joinToString("\n")
    }
}
