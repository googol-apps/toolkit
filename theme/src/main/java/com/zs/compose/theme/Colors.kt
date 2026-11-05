package com.zs.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp

/**
 * Core color palette for [AppTheme].
 *
 * Colors are organized into role-based pairs: each surface color (such as [accent]) has a matching
 * `on` color that guarantees legible content (text, icons) when drawn on top of it. Always pair
 * them, e.g. `onAccent` on `accent`, rather than mixing roles.
 *
 * The palette is deliberately minimal. Additional tones, such as shadows and elevated
 * backgrounds, are derived from these roles through the helper functions below instead of being
 * stored as separate values.
 *
 * @param accent Brand color that highlights active or important elements such as primary actions,
 *   selected states, and focus indicators.
 * @param onAccent Content color for use on top of [accent].
 * @param error Color that signals errors, destructive actions, or other negative states.
 * @param onError Content color for use on top of [error].
 * @param background Base surface color of the app.
 * @param onBackground Content color for use on top of [background].
 */
@Immutable
class Colors(
    val accent: Color,
    val onAccent: Color,
    val error: Color,
    val onError: Color,
    val background: Color,
    val onBackground: Color,
) {

    val isLight: Boolean get() = background.luminance() > 0.5f

    /**
     * Calculates a background color with an overlay based on the provided elevation.
     *
     * @param elevation The elevation value to calculate the overlay alpha for.
     * @return A [Color] representing the background with an overlay.*/
    @Stable
    fun background(elevation: Dp) =
        background.atTonalElevation(accent,  if (isLight) elevation else elevation * 0.5f)

    /** Returns a copy of this ColorScheme, optionally overriding some of the values. */
    fun copy(
        accent: Color = this.accent,
        onAccent: Color = this.onAccent,
        error: Color = this.error,
        onError: Color = this.onError,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
    ) = Colors(accent, onAccent, error, onError, background, onBackground)

    // Equals
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Colors

        if (accent != other.accent) return false
        if (onAccent != other.onAccent) return false
        if (error != other.error) return false
        if (onError != other.onError) return false
        if (background != other.background) return false
        if (onBackground != other.onBackground) return false

        return true
    }
    // hashcode
    override fun hashCode(): Int {
        var result = accent.hashCode()
        result = 31 * result + onAccent.hashCode()
        result = 31 * result + error.hashCode()
        result = 31 * result + onError.hashCode()
        result = 31 * result + background.hashCode()
        result = 31 * result + onBackground.hashCode()
        return result
    }
    // stringfy
    override fun toString(): String {
        return "Colors(accent=$accent, onAccent=$onAccent, error=$error, onError=$onError, background=$background, onBackground=$onBackground)"
    }
}
