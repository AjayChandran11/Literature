package com.cards.game.literature.ui.common

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.window.core.layout.WindowSizeClass

/**
 * Convenience accessors for the current window's adaptive info.
 *
 * Layout decisions are **width-driven** per Material 3 guidance:
 *  - Compact  (< 600dp)  → phone portrait: tabs + bottom nav
 *  - Medium   (600–839dp) → phone landscape / small tablet: side-by-side, compact components
 *  - Expanded (≥ 840dp)   → tablet landscape / foldable unfolded: side-by-side, full-size components
 *
 * Height is used for **secondary** compactness tweaks:
 *  - Compact  (< 480dp)   → phone landscape: compact header, smaller buttons
 *
 * Uses [WindowSizeClass.isWidthAtLeastBreakpoint] / [WindowSizeClass.isHeightAtLeastBreakpoint]
 * (the non-deprecated API) with the standard Material breakpoints.
 */
object WindowSize {

    // Standard Material breakpoints (dp)
    private const val MEDIUM_WIDTH_BREAKPOINT = 600
    private const val EXPANDED_WIDTH_BREAKPOINT = 840
    private const val MEDIUM_HEIGHT_BREAKPOINT = 480

    /** Whether to use side-by-side layout (width >= 600dp — Medium or Expanded). */
    val WindowAdaptiveInfo.useSideBySide: Boolean
        get() = windowSizeClass.isWidthAtLeastBreakpoint(MEDIUM_WIDTH_BREAKPOINT)

    /** Phone landscape: very little vertical space (height < 480dp). */
    val WindowAdaptiveInfo.isCompactHeight: Boolean
        get() = !windowSizeClass.isHeightAtLeastBreakpoint(MEDIUM_HEIGHT_BREAKPOINT)

    /** Tablet-class width (≥ 840dp): show full-size components, generous padding. */
    val WindowAdaptiveInfo.isExpandedWidth: Boolean
        get() = windowSizeClass.isWidthAtLeastBreakpoint(EXPANDED_WIDTH_BREAKPOINT)
}
