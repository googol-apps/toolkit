package com.zs.compose.foundation.decorator

import androidx.annotation.FloatRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.unit.EdgeInsets


private class DecoratorElement(
    // background
    val backgroundColor: Color,
    val backgroundBrush: Brush?,
    val backgroundAlpha: Float,

    // foreground
    val foregroundColor: Color,
    val foregroundBrush: Brush?,
    val foregroundAlpha: Float,

    // shape
    val shape: Shape,
    //
    val borderColor: Color,
    val borderBrush: Brush?,
    val borderWidth: Dp,
    val borderPathEffect: PathEffect?,

    // Outline styling (for styling that strictly traces the outer contour of the content.)
    var outlineColor: Color,
    var outlineGap: Dp,
    var outlineBrush: Brush?,
    var outlineWidth: Dp,
    var outlinePathEffect: PathEffect? = null, // e.g. dashed outine

    //
    val elevation: Dp,

    // scale
    @FloatRange(from = 0.0, to = 1.0) val scaleX: Float,
    @FloatRange(from = 0.0, to = 1.0) var scaleY: Float,

    //
    val roughness: Float,
    // content padding
    val edgeInsets: EdgeInsets,
) : ModifierNodeElement<DecoratorNode>() {
    override fun create(): DecoratorNode = DecoratorNode(
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


        outlineColor = outlineColor,
        outlineGap = outlineGap,
        outlineBrush = outlineBrush,
        outlineWidth = outlineWidth,
        outlinePathEffect = outlinePathEffect,

        //
        elevation = elevation,

        //
        scaleX = scaleX,
        scaleY = scaleY,

        //
        roughness = roughness,
        //
        edgeInsets = edgeInsets
    )

    override fun update(node: DecoratorNode) {
        // background
        node.backgroundColor = backgroundColor
        node.backgroundBrush = backgroundBrush
        node.backgroundAlpha = backgroundAlpha

        // foreground
        node.foregroundColor = foregroundColor
        node.foregroundBrush = foregroundBrush
        node.foregroundAlpha = foregroundAlpha

        // border
        if (node.borderWidth != borderWidth || node.borderPathEffect != borderPathEffect)
            node.borderStroke = null // reset
        node.borderColor = borderColor
        node.borderBrush = borderBrush
        node.borderWidth = borderWidth
        // outline stroke
        if (node.outlineWidth != outlineWidth || node.outlinePathEffect != outlinePathEffect)
            node.outlineStroke = null // reset
        node.outlineColor = outlineColor
        node.outlineGap = outlineGap
        node.outlineBrush = outlineBrush
        node.outlineWidth = outlineWidth
        node.outlinePathEffect = outlinePathEffect

        //
        node.elevation = node.elevation
        //
        node.scaleX = scaleX
        node.scaleY = scaleY
        // roughness
        if (node.roughness <= 0) node.noiseEffectBrush = null // clean up.
        node.roughness = roughness

        //
        node.edgeInsets = edgeInsets
        if (node.shape != shape) {
            node.shape = shape
            node.invalidateSemantics()
        }
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {

    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DecoratorElement

        if (backgroundAlpha != other.backgroundAlpha) return false
        if (foregroundAlpha != other.foregroundAlpha) return false
        if (scaleX != other.scaleX) return false
        if (scaleY != other.scaleY) return false
        if (roughness != other.roughness) return false
        if (backgroundColor != other.backgroundColor) return false
        if (backgroundBrush != other.backgroundBrush) return false
        if (foregroundColor != other.foregroundColor) return false
        if (foregroundBrush != other.foregroundBrush) return false
        if (shape != other.shape) return false
        if (borderColor != other.borderColor) return false
        if (borderBrush != other.borderBrush) return false
        if (borderWidth != other.borderWidth) return false
        if (borderPathEffect != other.borderPathEffect) return false
        if (outlineColor != other.outlineColor) return false
        if (outlineGap != other.outlineGap) return false
        if (outlineBrush != other.outlineBrush) return false
        if (outlineWidth != other.outlineWidth) return false
        if (outlinePathEffect != other.outlinePathEffect) return false
        if (elevation != other.elevation) return false
        if (edgeInsets != other.edgeInsets) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundAlpha.hashCode()
        result = 31 * result + foregroundAlpha.hashCode()
        result = 31 * result + scaleX.hashCode()
        result = 31 * result + scaleY.hashCode()
        result = 31 * result + roughness.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + (backgroundBrush?.hashCode() ?: 0)
        result = 31 * result + foregroundColor.hashCode()
        result = 31 * result + (foregroundBrush?.hashCode() ?: 0)
        result = 31 * result + shape.hashCode()
        result = 31 * result + borderColor.hashCode()
        result = 31 * result + (borderBrush?.hashCode() ?: 0)
        result = 31 * result + borderWidth.hashCode()
        result = 31 * result + (borderPathEffect?.hashCode() ?: 0)
        result = 31 * result + outlineColor.hashCode()
        result = 31 * result + outlineGap.hashCode()
        result = 31 * result + (outlineBrush?.hashCode() ?: 0)
        result = 31 * result + outlineWidth.hashCode()
        result = 31 * result + (outlinePathEffect?.hashCode() ?: 0)
        result = 31 * result + elevation.hashCode()
        result = 31 * result + edgeInsets.hashCode()
        return result
    }
}

/**
 * A highly optimized, consolidated modifier that applies common visual decorations to a component.
 *
 * Instead of chaining multiple modifiers like `Modifier.background()`, `Modifier.border()`,
 * `Modifier.clip()`, and `Modifier.shadow()`, this modifier handles all these aspects in a single
 * draw pass using a [GraphicsLayer].
 *
 * ### Key Benefits:
 * 1. **Performance**: Reduces the depth of the modifier tree and minimizes recomposition overhead.
 * 2. **Caching**: Automatically caches the [Outline] of the [shape] to prevent expensive path
 * recalculations on every frame, only invalidating when the size, layout direction, or shape state changes.
 * 3. **Layer-based Elevation**: Uses hardware-accelerated [GraphicsLayer] for shadow elevation.
 * 4. **Unified Foreground**: Supports drawing a foreground color or brush over the content, useful
 * for overlays or state-based dimming.
 *
 * @param backgroundColor The solid color to draw as the background.
 * @param backgroundBrush The optional brush for complex background patterns (gradients, etc.).
 * @param backgroundAlpha The opacity of the [backgroundBrush].
 * @param foregroundColor A solid color overlay drawn on top of the content.
 * @param foregroundBrush A brush overlay drawn on top of the content.
 * @param foregroundAlpha The opacity of the [foregroundBrush].
 * @param shape The geometric shape used for the background, border, clipping, and shadow.
 * Defaults to [RectangleShape].
 * @param borderColor The solid color of the border.
 * @param borderBrush The brush used for the border (e.g., a gradient border).
 * @param borderWidth The thickness of the border. If [Dp.Unspecified], no border is drawn.
 * @param elevation The size of the shadow below the component.
 * @param noiseAlpha Controls the grain effect intensity. Value ranges from 0 (off) to 1.
 * @param edgeInsets The additional space to be applied around the component's boundaries. Defaults to [EdgeInsets(0.dp)].
 */
@Stable
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
) = this then DecoratorElement(
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
    roughness = noiseAlpha,

    //
    edgeInsets = edgeInsets
)

/**
 * @see decorator
 */
@Stable
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
    edgeInsets: EdgeInsets = com.zs.compose.foundation.unit.EdgeInsets(),
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