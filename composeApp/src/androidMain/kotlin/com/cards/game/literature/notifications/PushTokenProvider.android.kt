package com.cards.game.literature.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

actual fun requestPushToken() {
    // Firebase can be absent (a build without google-services.json) or refuse (no Play services
    // on the device); either way there is simply no push, never a crash on sitting down.
    runCatching {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { PushTokens.set(it) }
            .addOnFailureListener { Log.w("Push", "No push token: ${it.message}") }
    }.onFailure { Log.w("Push", "Push unavailable: ${it.message}") }
}
