package com.zs.compose.foundation.decorator

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.style.styleable
import com.zs.compose.foundation.unit.EdgeInsets

/**
 * @see styleable
 */
@Stable
@Deprecated(
    "Replace with the new styleable modifier",
)
fun Modifier.decorator(
    // background
    backgroundColor: Color,
    backgroundBrush: Brush? = null,
    @FloatRange(from = 0.0, to = 1.0) backgroundAlpha: Float = 1.0f,

    // foreground
    foregroundColor: Color = Color.Unspecified,
    foregroundBrush: Brush? = null,
    @FloatRange(from = 0.0, to = 1.0) foregroundAlpha: Float = 1f,

    // shape
    shape: Shape = RectangleShape,
    // border
    border: BorderStroke? = null,
    borderPathEffect: PathEffect? = null,
    //
    outline: BorderStroke? = null,
    outlineGap: Dp = 0.dp,
    outlinePathEffect: PathEffect? = null,

    //
    elevation: Dp = Dp.Unspecified,
    @FloatRange(from = 0.0, to = 1.0) scale: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) noiseAlpha: Float = 0.0f,
    //
    edgeInsets: EdgeInsets = EdgeInsets(),
) = decorator(
    // background
    backgroundColor = backgroundColor,
    backgroundBrush = backgroundBrush,
    backgroundAlpha = backgroundAlpha,

    // foreground
    foregroundColor = foregroundColor,
    foregroundBrush = foregroundBrush,
    foregroundAlpha = foregroundAlpha,

    // shape
    shape = shape,
    //
    borderBrush = border?.brush,
    borderWidth = border?.width ?: Dp.Unspecified,
    borderPathEffect = borderPathEffect,
    //
    outlineColor = Color.Unspecified,
    outlineGap = outlineGap,
    outlineBrush = outline?.brush,
    outlineWidth = outline?.width ?: 0.dp,
    outlinePathEffect = outlinePathEffect,

    //
    scaleX = scale,
    scaleY = scale,
    //
    elevation = elevation,
    //
    noiseAlpha = noiseAlpha,
    //
    edgeInsets = edgeInsets
)

/**
 * @see Modifier.styleable
 */
@Stable
@Deprecated("Replace with the new styleable modifier")
fun Modifier.decorator(
    // background
    backgroundColor: Color,
    backgroundBrush: Brush? = null,
    @FloatRange(from = 0.0, to = 1.0) backgroundAlpha: Float = 1.0f,

    // foreground
    foregroundColor: Color = Color.Unspecified,
    foregroundBrush: Brush? = null,
    @FloatRange(from = 0.0, to = 1.0) foregroundAlpha: Float = 1f,

    // shape
    shape: Shape = RectangleShape,
    //
    borderColor: Color = Color.Unspecified,
    borderBrush: Brush? = null,
    borderWidth: Dp = Dp.Unspecified,
    borderPathEffect: PathEffect? = null,

    //
    outlineColor: Color = Color.Unspecified,
    outlineGap: Dp = 0.dp,
    outlineBrush: Brush? = null,
    outlineWidth: Dp = 0.dp,
    outlinePathEffect: PathEffect? = null,

    //
    elevation: Dp = Dp.Unspecified,

    // scale
    @FloatRange(from = 0.0, to = 1.0) scaleX: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) scaleY: Float = 1.0f,

    // noise
    @FloatRange(from = 0.0, to = 1.0) noiseAlpha: Float = 0.0f,

    // insets
    edgeInsets: EdgeInsets = com.zs.compose.foundation.unit.EdgeInsets()
) = styleable(
    // background
    backgroundColor = backgroundColor,
    backgroundBrush = backgroundBrush,
    backgroundAlpha = backgroundAlpha,

    // foreground
    foregroundColor = foregroundColor,
    foregroundBrush = foregroundBrush,
    foregroundAlpha = foregroundAlpha,

    // shape
    shape = shape,
    //
    borderColor = borderColor,
    borderBrush = borderBrush,
    borderWidth = borderWidth,
    borderPathEffect = borderPathEffect,
    //
    outlineColor = outlineColor,
    outlineGap = outlineGap,
    outlineBrush = outlineBrush,
    outlineWidth = outlineWidth,
    outlinePathEffect = outlinePathEffect,

    //
    elevation = elevation,

    // scale
    scaleX = scaleX,
    scaleY = scaleY,

    //
    noiseAlpha = noiseAlpha,

    //
    edgeInsets = edgeInsets
)