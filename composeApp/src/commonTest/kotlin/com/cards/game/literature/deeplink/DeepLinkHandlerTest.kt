package com.cards.game.literature.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkHandlerTest {

    // ── Install-referrer parsing (join.html writes "room=<CODE>" on the Play URL) ──

    @Test
    fun referrer_plainRoom() {
        assertEquals("ABC123", DeepLinkHandler.extractRoomCodeFromReferrer("room=ABC123"))
    }

    @Test
    fun referrer_lowercaseIsUppercased() {
        assertEquals("ABC123", DeepLinkHandler.extractRoomCodeFromReferrer("room=abc123"))
    }

    @Test
    fun referrer_withUtmParams() {
        assertEquals(
            "XYZ789",
            DeepLinkHandler.extractRoomCodeFromReferrer("room=XYZ789&utm_source=whatsapp&utm_medium=social"),
        )
    }

    @Test
    fun referrer_organicHasNoRoom() {
        assertNull(DeepLinkHandler.extractRoomCodeFromReferrer("utm_source=google-play&utm_medium=organic"))
    }

    @Test
    fun referrer_emptyOrNull() {
        assertNull(DeepLinkHandler.extractRoomCodeFromReferrer(""))
        assertNull(DeepLinkHandler.extractRoomCodeFromReferrer(null))
    }

    @Test
    fun referrer_rejectsWrongLength() {
        assertNull(DeepLinkHandler.extractRoomCodeFromReferrer("room=ABC12"))   // 5 chars
        assertNull(DeepLinkHandler.extractRoomCodeFromReferrer("room=ABC1234")) // 7 chars
    }

    // ── Sanity: the underlying extractor still handles the deep-link forms ──

    @Test
    fun extract_customScheme() {
        assertEquals("ABC123", DeepLinkHandler.extractRoomCode("literature://join?room=ABC123"))
    }

    @Test
    fun extract_appLink() {
        assertEquals(
            "ABC123",
            DeepLinkHandler.extractRoomCode("https://ajaychandran11.github.io/Literature/join.html?room=ABC123"),
        )
    }

    @Test
    fun extract_bareCode() {
        assertEquals("ABC123", DeepLinkHandler.extractRoomCode("ABC123"))
    }
}
