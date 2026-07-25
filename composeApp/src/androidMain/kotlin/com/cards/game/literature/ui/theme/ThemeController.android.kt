package com.cards.game.literature.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

actual val isDynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun SystemBarsEffect(darkTheme: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(darkTheme) {
        val activity = context.findComponentActivity() ?: return@LaunchedEffect
        // Re-issue edge-to-edge with an explicit style so bar icon contrast follows
        // the app's chosen theme, not the OS one (they differ when forced).
        val style = if (darkTheme) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is Activity -> null
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
