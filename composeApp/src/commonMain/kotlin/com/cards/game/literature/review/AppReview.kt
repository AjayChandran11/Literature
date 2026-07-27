package com.cards.game.literature.review

/**
 * In-app review prompt. Android drives the Play In-App Review flow; iOS is a no-op today.
 * Fire-and-forget and always safe to call — Play decides whether to actually show the sheet
 * (and rate-limits it by its own quota), so callers only need to pick a positive moment.
 */
expect object AppReview {
    fun requestReview()
}
