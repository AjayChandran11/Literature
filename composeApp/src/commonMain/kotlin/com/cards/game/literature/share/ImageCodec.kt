package com.cards.game.literature.share

import androidx.compose.ui.graphics.ImageBitmap

/** Encodes a captured [ImageBitmap] to PNG bytes. Platform-specific because the
 *  underlying bitmap conversion (e.g. asAndroidBitmap) is not in common code. */
expect fun imageBitmapToPng(bitmap: ImageBitmap): ByteArray
