package com.cards.game.literature.share

// Web Share API where available (mobile browsers); otherwise hand the text to WhatsApp Web.
private fun jsShareText(text: String): Unit = js(
    "navigator.share ? navigator.share({ text: text }) : window.open('https://wa.me/?text=' + encodeURIComponent(text), '_blank')"
)

actual object Sharer {
    actual fun shareText(text: String) {
        jsShareText(text)
    }

    // Image sharing needs a PNG encoder first (see ImageCodec); share the caption for now.
    actual fun shareImage(pngBytes: ByteArray, caption: String) {
        shareText(caption)
    }

    actual fun isWhatsAppAvailable(): Boolean = false

    actual fun shareTextToWhatsApp(text: String): Boolean = false
}
