package com.cards.game.literature.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import java.lang.ref.WeakReference

actual object AppReview {
    // Weakly held so a finished Activity can still be GC'd; the review flow needs a live Activity
    // (not the application context), and requests only fire while the app is foregrounded.
    private var activityRef: WeakReference<Activity>? = null

    /** Called by MainActivity so the review flow has an Activity to launch from. */
    fun setActivity(activity: Activity?) {
        activityRef = activity?.let { WeakReference(it) }
    }

    actual fun requestReview() {
        val activity = activityRef?.get() ?: return
        try {
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                // On failure (over quota, no Play Store, offline) just do nothing — Play never
                // reports whether a review was actually left, so there's no follow-up either way.
                if (task.isSuccessful) {
                    manager.launchReviewFlow(activity, task.result)
                }
            }
        } catch (_: Exception) {
            // Defensive: a review prompt must never crash the app.
        }
    }
}
