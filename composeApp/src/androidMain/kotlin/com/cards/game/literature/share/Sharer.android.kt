package com.cards.game.literature.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

actual object Sharer {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun shareText(text: String) {
        val ctx = appContext ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // Started from the application context, so the chooser needs its own task.
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ctx.startActivity(chooser)
        } catch (_: Exception) {
            // No app available to handle the share — fail silently.
        }
    }

    actual fun shareImage(pngBytes: ByteArray, caption: String) {
        val ctx = appContext ?: return
        try {
            // Single overwritten file under cacheDir/shared_images (matches file_paths.xml).
            val dir = File(ctx.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(dir, "result_card.png")
            file.writeBytes(pngBytes)
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)

            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, null).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(chooser)
        } catch (_: Exception) {
            // Encoding/IO/no-handler — fail silently, matching shareText.
        }
    }
}
