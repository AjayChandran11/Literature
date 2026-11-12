package com.cards.game.literature.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * This device's push token, as the platform last reported it: null where there is no push
 * service (web, iOS today) and until Firebase has answered on Android. The online repository
 * hands it to the server for the seat it holds, and again whenever it changes.
 */
object PushTokens {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    fun set(token: String?) {
        _token.value = token?.takeIf { it.isNotBlank() }
    }
}

/**
 * Asks the platform for its push token; the answer lands in [PushTokens] asynchronously. A no-op
 * where there is no push service. Only called once the player sits down at an online table, so
 * a phone that never plays online is never registered with the push service at all.
 */
expect fun requestPushToken()
