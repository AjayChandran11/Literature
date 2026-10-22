package com.cards.game.literature.deeplink

import com.cards.game.literature.isWebPlatform

/**
 * Builds the public invite URL for a room. This is an https App Link served from the
 * project's GitHub Pages site. When the app is installed (and the link is verified via
 * /.well-known/assetlinks.json on the domain root), tapping it opens the app straight
 * into the room; otherwise it lands on join.html which routes to the Play Store.
 *
 * The host must match the App Link intent-filter in AndroidManifest.xml and the
 * assetlinks.json statement.
 */
object InviteLink {
    private const val BASE = "https://ajaychandran11.github.io/Literature/join.html"

    /** Google Play listing — the acquisition CTA for the shareable result card.
     *  (Not a room link: the room is finished by results time.) */
    const val PLAY_STORE = "https://play.google.com/store/apps/details?id=com.cards.game.literature"

    /** The web client, for people already in a browser. */
    const val WEB_APP = "https://literature-game.pages.dev"

    /** Opens today's puzzle in the web client; the number is for the reader and for analytics. */
    fun forPuzzle(puzzleNumber: Int): String = "$WEB_APP/?puzzle=$puzzleNumber"

    /** Where a share should send someone who wants the app: the web client when shared from the
     *  browser build (a store link there sends a desktop reader to a phone store), Play otherwise. */
    val appLink: String get() = if (isWebPlatform()) WEB_APP else PLAY_STORE

    /** e.g. https://ajaychandran11.github.io/Literature/join.html?room=ABC123 */
    fun forRoom(roomCode: String): String = "$BASE?room=$roomCode"
}
