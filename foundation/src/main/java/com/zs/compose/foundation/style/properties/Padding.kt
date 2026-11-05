package com.zs.compose.foundation.style.properties

import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.ui.unit.LayoutDirection as Direction
import androidx.compose.ui.unit.lerp as lerpDp

/**
 * Lazily interpolates padding on demand.
 *
 * Individual edges are resolved using the current layout direction,
 * allowing directional paddings to animate correctly.
 */
private class StylePadding(private val from: Padding, private val to: Padding, private val fraction: Float) : Padding {

    //Resolve and interpolate the effective left padding.
    override fun calculateLeftPadding(direction: Direction): Dp =
        lerpDp(from.calculateLeftPadding(direction), to.calculateLeftPadding(direction), fraction)

    override fun calculateTopPadding(): Dp =
        lerpDp(from.calculateTopPadding(), to.calculateTopPadding(), fraction)

    override fun calculateRightPadding(direction: Direction): Dp =
        // Resolve and interpolate the effective right padding.
        lerpDp(from.calculateRightPadding(direction), to.calculateRightPadding(direction), fraction)

    override fun calculateBottomPadding(): Dp =
        lerpDp(from.calculateBottomPadding(), to.calculateBottomPadding(), fraction)

    // Value-based equality for stability and debugging.
    override fun equals(other: Any?): Boolean {
        return other is StylePadding &&
                from == other.from &&
                to == other.to &&
                fraction == other.fraction
    }

    override fun hashCode(): Int =
        (from.hashCode() * 31 + to.hashCode()) * 31 + fraction.hashCode()
}

/**
 * Interpolates between two [Padding] values.
 */
internal fun lerp(start: Padding, stop: Padding, fraction: Float): Padding {
    return when {
        fraction <= 0f -> start // Fast path: fully at start state.
        fraction >= 1f -> stop  // Fast path: fully at end state.
        else -> StylePadding(start, stop, fraction)
    }
}