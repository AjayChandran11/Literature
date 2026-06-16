package com.cards.game.literature.share

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun imageBitmapToPng(bitmap: ImageBitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}
