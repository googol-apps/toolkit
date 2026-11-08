@file:OptIn(ExperimentalFoundationStyleApi::class)

package com.zs.compose.theme

import androidx.compose.foundation.style.CommonStyleScope
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.Amber
import com.zs.compose.foundation.CharcoalBlack
import com.zs.compose.foundation.RedViolet
import com.zs.compose.foundation.Rose
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.UmbraGrey

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

    /**
     * Returns `true` when the theme background colour is considered light.
     *
     * This is determined by the background colour's relative luminance.
     * A luminance value greater than `0.5f` is treated as a light background;
     * otherwise the background is considered dark.
     */
    val isLight: Boolean
        get() = background.luminance() > 0.5f

    /**
     * Calculates a background color with an overlay based on the provided elevation.
     *
     * @param elevation The elevation value to calculate the overlay alpha for.
     * @return A [Color] representing the background with an overlay.*/
    @Stable
    fun background(elevation: Dp) =
        background.atTonalElevation(accent, if (isLight) elevation else elevation * 0.5f)

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

/**
 * Creates a complete color definition for the [AppTheme] using the default light theme values.
 * @see darkColors
 */
@Stable
fun lightColors(
    accent: Color = Color(0xFF6750A4),
    onAccent: Color = Color.SignalWhite,
    error: Color = Color.RedViolet,
    onError: Color = Color.SignalWhite,
    background: Color = Color.White.atTonalElevation(accent, 1.dp),
    onBackground: Color = Color.UmbraGrey,
) = Colors(accent, onAccent, error, onError, background, onBackground)

/**
 * @see lightColors
 */
@Stable
fun darkColors(
    accent: Color = Color.Amber,
    onAccent: Color = Color.SignalWhite,
    error: Color = Color.Rose,
    onError: Color = Color.SignalWhite,
    background: Color = Color.CharcoalBlack,
    onBackground: Color = Color.SignalWhite,
) = Colors(accent, onAccent, error, onError, background, onBackground)

/**
 * CompositionLocal containing the preferred content color for a given position in the hierarchy.
 * This typically represents the `on` color for a color in [Colors]. For example, if the
 * background color is [Colors.background], this color is typically set to
 * [Colors.onBackground].
 *
 * This color should be used for any typography / iconography, to ensure that the color of these
 * adjusts when the background color changes. For example, on a dark background, text should be
 * light, and on a light background, text should be dark.
 *
 * Defaults to [Color.Black] if no color has been explicitly set.
 */
val LocalContentColor: ProvidableCompositionLocal<Color> = compositionLocalOf { Color.Black }

/**
 * CompositionLocal used to pass [Colors] down the tree.
 *
 * Setting the value here is typically done as part of [AppTheme], which will automatically
 * handle efficiently updating any changed colors without causing unnecessary recompositions.
 */
internal val LocalColors = staticCompositionLocalOf(::lightColors)

/**
 * The current [Colors] of [AppTheme], readable inside a style block.
 *
 * Reads are observed by the style resolver, so [LocalColors] should be a
 * `compositionLocalOf` for theme changes to restyle components.
 */
val CommonStyleScope.colors: Colors
    get() = LocalColors.currentValue