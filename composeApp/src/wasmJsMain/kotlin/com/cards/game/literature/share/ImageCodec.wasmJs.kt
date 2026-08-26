package com.cards.game.literature.share

import androidx.compose.ui.graphics.ImageBitmap

// PNG encoding on web is deferred (mirrors iOS); callers fall back to text sharing.
actual fun imageBitmapToPng(bitmap: ImageBitmap): ByteArray = ByteArray(0)
