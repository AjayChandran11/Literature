package com.cards.game.literature.deeplink

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

    /** e.g. https://ajaychandran11.github.io/Literature/join.html?room=ABC123 */
    fun forRoom(roomCode: String): String = "$BASE?room=$roomCode"
}
