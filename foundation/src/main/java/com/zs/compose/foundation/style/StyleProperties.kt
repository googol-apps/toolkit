@file:OptIn(ExperimentalFoundationStyleApi::class)

package com.zs.compose.foundation.style

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.style.CommonStyleScope
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Fill
import androidx.compose.foundation.style.ProvidableStyleProperty
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.StyleResolver
import androidx.compose.foundation.style.size
import androidx.compose.foundation.style.stylePropertyOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.lerp as lerpOffset
import androidx.compose.ui.unit.lerp as lerpDpOffset

internal val StyleBackgroundProperty = stylePropertyOf("background") { Fill.None }
internal val StyleForegroundProperty = stylePropertyOf("foreground") { Fill.None }
internal val StyleShapeProperty = stylePropertyOf("shape") { RectangleShape }
internal val StylePaddingProperty = stylePropertyOf("padding") { PaddingValues.Zero }
internal val StyleElevationProperty = stylePropertyOf("elevation") { Dp.Unspecified }
internal val StyleBlurRadiusProperty = stylePropertyOf("blur_radius") { 0f }
internal val StyleBorderProperty = stylePropertyOf("border") { BorderStroke(0.dp, Color.Unspecified) }
internal val StyleScaleProperty = stylePropertyOf("scale", ::lerpOffset){ Offset(1f, 1f) }
internal val StyleTranslateProperty = stylePropertyOf("translate", ::lerpDpOffset){ DpOffset.Zero }
internal val StyleNoiseProperty = stylePropertyOf("noise") { 0f }

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
fun CommonStyleScope.padding(values: PaddingValues) =
    StylePaddingProperty.provide(values)

fun CommonStyleScope.padding(all: Dp) =
    StylePaddingProperty.provide(PaddingValues(all))

fun CommonStyleScope.padding(start: Dp, top: Dp, end: Dp, bottom: Dp) =
    StylePaddingProperty.provide(PaddingValues(start, top, end, bottom))

fun CommonStyleScope.padding(horizontal: Dp, vertical: Dp) =
    StylePaddingProperty.provide(PaddingValues(horizontal, vertical))

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
fun CommonStyleScope.scale(@FloatRange(from = 0.0) scaleX: Float, @FloatRange(from = 0.0) scaleY: Float) =
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
 * Applies a noise effect with the specified [intensity].
 */
fun CommonStyleScope.noise(@FloatRange(from = 0.0, to = 1.0) intensity: Float) =
    StyleNoiseProperty.provide(intensity.coerceIn(0f, 1f))
