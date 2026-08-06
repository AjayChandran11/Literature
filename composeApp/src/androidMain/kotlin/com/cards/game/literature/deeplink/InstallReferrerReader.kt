package com.cards.game.literature.deeplink

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.cards.game.literature.analytics.Analytics
import com.cards.game.literature.analytics.AnalyticsEvent

/**
 * One-time reader for the Play Install Referrer.
 *
 * When someone without the app taps a room invite, `join.html` sends them to Play with
 * `&referrer=room=<CODE>`. On the first launch after they install, we read that back and
 * feed the code into [DeepLinkHandler] — the same sink App Links use — so the room invite
 * surfaces on Home once onboarding is done.
 *
 * Android-only: the App Store has no equivalent, so there is no iOS counterpart.
 */
object InstallReferrerReader {
    private const val PREFS = "lit_prefs"
    private const val KEY_CONSUMED = "install_referrer_consumed"

    /**
     * Reads the install referrer at most once per install. Safe to call on every cold start —
     * it self-gates on a persisted flag and no-ops once consumed.
     *
     * @param hasPendingInvite true when an explicit deep link already delivered a room code
     *   this launch; we then skip the referrer so it can't clobber the real invite.
     */
    fun checkOnce(context: Context, hasPendingInvite: Boolean) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_CONSUMED, false)) return

        val client = InstallReferrerClient.newBuilder(appContext).build()
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                try {
                    when (responseCode) {
                        InstallReferrerClient.InstallReferrerResponse.OK -> {
                            // The referrer is fixed for the install's lifetime — once we've read
                            // it we never need to again, so mark consumed regardless of contents.
                            val referrer = client.installReferrer.installReferrer
                            if (!hasPendingInvite) {
                                DeepLinkHandler.extractRoomCodeFromReferrer(referrer)?.let { code ->
                                    Analytics.log(AnalyticsEvent.InstallReferrerJoin)
                                    DeepLinkHandler.submit(code)
                                }
                            }
                            prefs.edit().putBoolean(KEY_CONSUMED, true).apply()
                        }
                        // Referrer API will never be available here — don't keep retrying.
                        InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                            prefs.edit().putBoolean(KEY_CONSUMED, true).apply()
                        // SERVICE_UNAVAILABLE is transient — leave unconsumed to retry next launch.
                    }
                } catch (_: Exception) {
                    // Dead/again-throwing client (RemoteException etc.) — leave unconsumed; retry later.
                } finally {
                    try { client.endConnection() } catch (_: Exception) {}
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Transient disconnect; nothing to do — the next cold start retries (still unconsumed).
            }
        })
    }
}
