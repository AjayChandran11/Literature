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

    // Ordered by preference: consumer WhatsApp first, then WhatsApp Business.
    private val whatsAppPackages = listOf("com.whatsapp", "com.whatsapp.w4b")

    /** The first installed WhatsApp package, or null. Relies on the `<queries>` entries in the
     *  manifest — without them getPackageInfo throws for these packages on Android 11+. */
    private fun installedWhatsApp(): String? {
        val pm = appContext?.packageManager ?: return null
        return whatsAppPackages.firstOrNull { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    actual fun isWhatsAppAvailable(): Boolean = installedWhatsApp() != null

    actual fun shareTextToWhatsApp(text: String): Boolean {
        val ctx = appContext ?: return false
        val pkg = installedWhatsApp() ?: return false
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            ctx.startActivity(send)
            true
        } catch (_: Exception) {
            // Uninstalled between the check and the tap, or otherwise unresolvable.
            false
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
