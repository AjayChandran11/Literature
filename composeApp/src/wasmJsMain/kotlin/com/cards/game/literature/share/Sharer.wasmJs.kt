package com.cards.game.literature.share

// wa.me works everywhere — WhatsApp app on phones, WhatsApp Web on desktop — so the waiting
// room's WhatsApp-first invite UI lights up on web too.
private fun openWhatsApp(text: String): Unit =
    js("window.open('https://wa.me/?text=' + encodeURIComponent(text), '_blank')")

private fun shareOrWhatsApp(text: String): Unit =
    js(
        "navigator.share ? navigator.share({ text: text }).catch(function(){}) : " +
            "window.open('https://wa.me/?text=' + encodeURIComponent(text), '_blank')"
    )

private fun hasClipboard(): Boolean =
    js("!!(navigator.clipboard && navigator.clipboard.writeText)")

private fun clipboardWrite(text: String): Unit =
    js("navigator.clipboard.writeText(text).catch(function(){})")

actual object Sharer {
    actual fun shareText(text: String) {
        shareOrWhatsApp(text)
    }

    // Result-card image sharing needs a PNG encoder on wasm first (see ImageCodec); caption only.
    actual fun shareImage(pngBytes: ByteArray, caption: String) {
        shareText(caption)
    }

    actual fun isWhatsAppAvailable(): Boolean = true

    actual fun shareTextToWhatsApp(text: String): Boolean {
        openWhatsApp(text)
        return true
    }

    // In-app WebViews (WhatsApp et al.) often lack the clipboard API — report that honestly
    // so the UI's "copied" confirmation never lies.
    actual fun copyText(text: String): Boolean {
        if (!hasClipboard()) return false
        clipboardWrite(text)
        return true
    }
}
