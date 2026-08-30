package com.cards.game.literature.share

/**
 * Platform share-sheet bridge. Used to invite friends to an online room.
 * Android stores the app context via init(); iOS presents a UIActivityViewController.
 */
expect object Sharer {
    /** Opens the system share sheet with [text] (plain text, e.g. an invite link). */
    fun shareText(text: String)

    /** Opens the system share sheet with a PNG image (e.g. a result card) plus a [caption]. */
    fun shareImage(pngBytes: ByteArray, caption: String)

    /** True when WhatsApp (or WhatsApp Business) is installed and can receive a share. Lets a
     *  caller show a WhatsApp-first affordance only when it will actually work. */
    fun isWhatsAppAvailable(): Boolean

    /** Sends [text] straight to WhatsApp, skipping the chooser. Returns false when WhatsApp
     *  isn't available (the caller should then fall back to [shareText]). */
    fun shareTextToWhatsApp(text: String): Boolean

    /** Puts [text] on the system clipboard. Returns false when unavailable. */
    fun copyText(text: String): Boolean
}
