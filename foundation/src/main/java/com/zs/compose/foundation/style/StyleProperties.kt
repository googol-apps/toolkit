@file:OptIn(ExperimentalFoundationStyleApi::class)

package com.zs.compose.foundation.style

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.style.Breadth
import androidx.compose.foundation.style.CommonStyleScope
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Fill
import androidx.compose.foundation.style.ProvidableStyleProperty
import androidx.compose.foundation.style.StyleResolver
import androidx.compose.foundation.style.stylePropertyOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isUnspecified
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.ui.geometry.lerp as lerpOffset
import androidx.compose.ui.unit.LayoutDirection as Direction
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.lerp as lerpDpOffset


/**
 * Lazily interpolates padding on demand.
 *
 * Individual edges are resolved using the current layout direction,
 * allowing directional paddings to animate correctly.
 */
private class LerpedPaddingValues(private val from: Padding, private val to: Padding, private val fraction: Float) : Padding {

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
        return other is LerpedPaddingValues &&
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
private fun lerpPadding(start: Padding, stop: Padding, fraction: Float): Padding {
    return when {
        fraction <= 0f -> start // Fast path: fully at start state.
        fraction >= 1f -> stop  // Fast path: fully at end state.
        else -> LerpedPaddingValues(start, stop, fraction)
    }
}

internal val StyleBackgroundProperty = stylePropertyOf("background") { Fill.None }
internal val StyleForegroundProperty = stylePropertyOf("foreground") { Fill.None }
internal val StyleShapeProperty = stylePropertyOf("shape") { RectangleShape }

internal val StylePaddingProperty = stylePropertyOf("padding", ::lerpPadding) { Padding.Zero }
internal val StyleElevationProperty = stylePropertyOf("elevation") { 0.dp }
internal val StyleBlurRadiusProperty = stylePropertyOf("blur_radius") { 0f }
internal val StyleBorderProperty = stylePropertyOf("border") { BorderStroke(0.dp, Color.Unspecified) }
internal val StyleScaleProperty = stylePropertyOf("scale", ::lerpOffset) { Offset(1f, 1f) }
internal val StyleTranslateProperty = stylePropertyOf("translate", ::lerpDpOffset) { DpOffset.Zero }
internal val StyleNoiseProperty = stylePropertyOf("noise") { 0f }
internal val StyleMinSizeProperty = stylePropertyOf("min_size", ::lerpDp) { DpSize.Zero }
internal val StyleWidthProperty = stylePropertyOf("width") { Breadth.None }
internal val StyleHeightProperty = stylePropertyOf("height") { Breadth.None }

internal val StyleMaxSizeProperty = stylePropertyOf(
    name = "max_size", defaultValue = { DpSize.Unspecified },
    interpolate = object : (DpSize, DpSize, Float) -> DpSize {
        override fun invoke(a: DpSize, b: DpSize, t: Float): DpSize =
            DpSize(snap(a.width, b.width, t), snap(a.height, b.height, t))

        private fun snap(a: Dp, b: Dp, t: Float): Dp = when {
            a.isUnspecified || b.isUnspecified -> if (t < 1f) a else b
            else -> lerpDp(a, b, t)
        }
    }
)

internal operator fun <T> StyleResolver.get(property: ProvidableStyleProperty<T>): T =
    resolve { property.value }

/** Sets the background to a solid [color]. */
fun CommonStyleScope.background(color: Color) =
    StyleBackgroundProperty.provide(Fill.Color(color))

/** Sets the background to a [brush]. */
fun CommonStyleScope.background(brush: Brush) =
    StyleBackgroundProperty.provide(Fill.Brush(brush))

/** Sets the foreground to a solid [color]. */
fun CommonStyleScope.foreground(color: Color) =
    StyleForegroundProperty.provide(Fill.Color(color))

/** Sets the foreground to a [brush]. */
fun CommonStyleScope.foreground(brush: Brush) =
    StyleForegroundProperty.provide(Fill.Brush(brush))

/** Sets the element [shape]. */
fun CommonStyleScope.shape(shape: Shape) =
    StyleShapeProperty.provide(shape)

/** Sets the padding to [values]. */
fun CommonStyleScope.padding(values: Padding) =
    StylePaddingProperty.provide(values)

fun CommonStyleScope.padding(all: Dp) =
    StylePaddingProperty.provide(Padding(all))

fun CommonStyleScope.padding(start: Dp, top: Dp, end: Dp, bottom: Dp) =
    StylePaddingProperty.provide(Padding(start, top, end, bottom))

fun CommonStyleScope.padding(horizontal: Dp, vertical: Dp) =
    StylePaddingProperty.provide(Padding(horizontal, vertical))

/** Sets the elevation to [elevation]. */
fun CommonStyleScope.elevation(elevation: Dp) =
    StyleElevationProperty.provide(elevation)

/** Sets the blur radius to [radius]. */
fun CommonStyleScope.blur(radius: Float) =
    StyleBlurRadiusProperty.provide(radius)

/**
 * Applies the given [BorderStroke] to this style scope.
 */
fun CommonStyleScope.border(border: BorderStroke) =
    StyleBorderProperty.provide(border)

/**
 * Applies a solid border with the specified [width] and [color].
 */
fun CommonStyleScope.border(width: Dp, color: Color) =
    StyleBorderProperty.provide(BorderStroke(width, color))

/**
 * Applies a brush-based border with the specified [width] and [brush].
 */
fun CommonStyleScope.border(width: Dp, brush: Brush) =
    StyleBorderProperty.provide(BorderStroke(width, brush))

/**
 * Uniformly scales this style on both the X and Y axes.
 */
fun CommonStyleScope.scale(@FloatRange(from = 0.0) scale: Float) =
    StyleScaleProperty.provide(Offset(scale, scale))

/**
 * Scales this style independently on the X and Y axes.
 */
fun CommonStyleScope.scale(
    @FloatRange(from = 0.0) scaleX: Float,
    @FloatRange(from = 0.0) scaleY: Float
) =
    StyleScaleProperty.provide(Offset(scaleX, scaleY))

/**
 * Translates this style on both the X and Y axes.
 */
fun CommonStyleScope.translate(offset: DpOffset) =
    StyleTranslateProperty.provide(offset)

/**
 * Translates this style on the X and Y axes.
 */
fun CommonStyleScope.translate(x: Dp, y: Dp) =
    StyleTranslateProperty.provide(DpOffset(x, y))

/**
 * Applies a noise effect over the background with the given [intensity].
 *
 * @param intensity strength of the noise, from `0f` (none) to `1f` (full). Values outside
 * this range are coerced.
 */
fun CommonStyleScope.noise(@FloatRange(from = 0.0, to = 1.0) intensity: Float) =
    StyleNoiseProperty.provide(intensity.coerceIn(0f, 1f))

/**
 * Sets the minimum size of the content, one value per axis.
 *
 * Padding is added outside this size. The minimum is clamped into the constraints coming from
 * the parent, so it cannot make the node larger than the parent allows.
 *
 * @param width minimum width. [Dp.Unspecified] or `0.dp` means no minimum.
 * @param height minimum height. [Dp.Unspecified] or `0.dp` means no minimum.
 */
fun CommonStyleScope.minSize(width: Dp, height: Dp) =
    minSize(DpSize(width, height))

/**
 * Sets the minimum size of the content.
 *
 * @param size minimum size. An unspecified or zero axis means no minimum on that axis.
 * @see maxSize
 */
fun CommonStyleScope.minSize(size: DpSize) =
    StyleMinSizeProperty.provide(size)

/**
 * Sets the maximum size of the content.
 *
 * @param size maximum size. An axis that is [Dp.Unspecified] (the default) has no limit.
 * If the maximum is smaller than the minimum, the minimum is lowered to match.
 * Animating to or from an unspecified axis snaps to the target value instead of interpolating.
 * @see minSize
 */
fun CommonStyleScope.maxSize(size: DpSize) =
    StyleMaxSizeProperty.provide(size)

/**
 * Sets the maximum size of the content, one value per axis.
 *
 * @param width maximum width. [Dp.Unspecified] means no limit.
 * @param height maximum height. [Dp.Unspecified] means no limit.
 */
fun CommonStyleScope.maxSize(width: Dp, height: Dp) =
    StyleMaxSizeProperty.provide(DpSize(width, height))

/**
 * Sets a fixed content width.
 *
 * The width is still limited by [minSize], [maxSize] and the parent's constraints, so the
 * resulting width can differ from [width] when those are tighter.
 *
 * @param width the preferred width.
 */
fun CommonStyleScope.width(width: Dp) =
    StyleWidthProperty.provide(Breadth(width))

/**
 * Sets a fixed content height.
 *
 * The height is still limited by [minSize], [maxSize] and the parent's constraints, so the
 * resulting height can differ from [height] when those are tighter.
 *
 * @param height the preferred height.
 */
fun CommonStyleScope.height(height: Dp) =
    StyleHeightProperty.provide(Breadth(height))

/**
 * Sets a fixed content width and height.
 *
 * Equivalent to calling [width] and [height].
 */
fun CommonStyleScope.size(width: Dp, height: Dp) {
    width(width)
    height(height)
}

/**
 * Makes the content width a [fraction] of the maximum width available.
 *
 * The fraction only applies when the parent gives a bounded width. Under an unbounded width
 * it is ignored. The result is clamped by [minSize] and [maxSize].
 *
 * @param fraction portion of the available width, from `0f` to `1f`.
 */
fun CommonStyleScope.fillMaxWidth(@FloatRange(0.0, 1.0) fraction: Float = 1.0f) =
    StyleWidthProperty.provide(Breadth(fraction))

/**
 * Makes the content height a [fraction] of the maximum height available.
 *
 * The fraction only applies when the parent gives a bounded height. Under an unbounded height
 * it is ignored. The result is clamped by [minSize] and [maxSize].
 *
 * @param fraction portion of the available height, from `0f` to `1f`.
 */
fun CommonStyleScope.fillMaxHeight(@FloatRange(0.0, 1.0) fraction: Float = 1.0f) =
    StyleHeightProperty.provide(Breadth(fraction))

/**
 * Makes the content fill a fraction of the available width and height.
 *
 * Equivalent to calling [fillMaxWidth] and [fillMaxHeight].
 *
 * @param width portion of the available width, from `0f` to `1f`.
 * @param height portion of the available height, from `0f` to `1f`.
 */
fun CommonStyleScope.fillMaxSize(
    @FloatRange(0.0, 1.0) width: Float = 1.0f,
    @FloatRange(0.0, 1.0) height: Float = 1.0f,
) {
    fillMaxWidth(width)
    fillMaxHeight(height)
}