package com.cards.game.literature.share

/**
 * Platform share-sheet bridge. Used to invite friends to an online room.
 * Android stores the app context via init(); iOS presents a UIActivityViewController.
 */
expect object Sharer {
    /** Opens the system share sheet with [text] (plain text, e.g. an invite link). */
    fun shareText(text: String)
}
