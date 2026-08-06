package com.cards.game.literature.ui.common

import com.cards.game.literature.model.Card
import com.cards.game.literature.model.Suit
import com.cards.game.literature.model.symbol

/**
 * Suit glyphs pinned to emoji presentation for UI text.
 *
 * The bare codepoints (U+2660..U+2666) default to *text* presentation, so which
 * glyph appears depends on the device's font fallback chain. On stock Android the
 * colour-emoji font wins and the suits render as the familiar glossy emoji — but a
 * user-selected font pack (Samsung "Font style", downloadable fonts) can supply its
 * own text glyphs for some of the four suits, hijacking shape and colour per suit
 * (hearts drawn in the Text tint instead of red, odd outlines, mixed styles).
 *
 * Appending VARIATION SELECTOR-16 (U+FE0F) forces emoji presentation, which always
 * resolves to the system emoji font — font packs never replace that — so every
 * device renders the same glyphs the app was designed around. Any `color`/`alpha`
 * on the Text keeps working the way it does today (emoji ignore tint, alpha fades).
 *
 * UI-only on purpose: [symbol]/`displayName` in the shared model stay bare so
 * engine logs, tests and wire formats are untouched.
 */
val Suit.emoji: String
    get() = symbol + EMOJI_VS

/** Card label for UI text ("A♥️", "7♠️") — [Card.displayName] with the suit pinned. */
val Card.displayEmoji: String
    get() = value.displayName + suit.emoji

/** VARIATION SELECTOR-16: forces emoji presentation of the preceding character. */
const val EMOJI_VS = "\uFE0F"
