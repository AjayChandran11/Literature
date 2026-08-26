package com.cards.game.literature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import literature.composeapp.generated.resources.Res
import literature.composeapp.generated.resources.noto_color_emoji
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont

internal fun removeSplash(): Unit = js("document.getElementById('splash')?.remove()")

/**
 * Registers a bundled Noto Color Emoji SUBSET (74 KB — just the suits, avatars and badges the
 * app uses) as a Skia fallback font before first paint, so those glyphs never flash as tofu.
 * The HTML splash stays up until the font is in. Glyphs outside the subset (e.g. emoji in
 * player names) still resolve through Compose 1.12's on-demand Noto download.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun WithEmojiFallback(content: @Composable () -> Unit) {
    val emojiFont by preloadFont(Res.font.noto_color_emoji)
    var ready by remember { mutableStateOf(false) }
    val resolver = LocalFontFamilyResolver.current
    LaunchedEffect(emojiFont) {
        emojiFont?.let {
            resolver.preload(FontFamily(it))
            ready = true
            removeSplash()
        }
    }
    if (ready) content()
}
