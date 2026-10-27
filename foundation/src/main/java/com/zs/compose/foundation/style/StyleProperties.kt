@file:OptIn(ExperimentalFoundationStyleApi::class)

package com.zs.compose.foundation.style

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val StyleBackgroundProperty = stylePropertyOf("background") { Fill.None }
internal val StyleForegroundProperty = stylePropertyOf("foreground") { Fill.None }
internal val StyleShapeProperty = stylePropertyOf("shape") { RectangleShape }
internal val StylePaddingProperty = stylePropertyOf("padding") { PaddingValues.Zero }
internal val StyleElevationProperty = stylePropertyOf("elevation") { Dp.Unspecified }
internal val StyleBlurRadiusProperty = stylePropertyOf("blur_radius") { Float.NaN }
internal val StyleBorderProperty = stylePropertyOf<BorderStroke>("border") { BorderStroke(0.dp,
    Color.Unspecified) }

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

/** Sets the elevation to [elevation]. */
fun CommonStyleScope.elevation(elevation: Dp) =
    StyleElevationProperty.provide(elevation)

/** Sets the blur radius to [radius]. */
fun CommonStyleScope.blur(radius: Float) =
    StyleBlurRadiusProperty.provide(radius)

fun CommonStyleScope.border(border: BorderStroke) =
    StyleBorderProperty.provide(border)


fun main() {
    val x = Style(){
       // size()
    }
}
