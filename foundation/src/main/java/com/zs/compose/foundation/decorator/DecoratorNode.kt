package com.zs.compose.foundation.decorator

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.setOutline
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.offset
import com.zs.compose.foundation.R
import com.zs.compose.foundation.requirePrecondition
import com.zs.compose.foundation.unit.EdgeInsets
import com.zs.compose.foundation.unit.calculateBottomInset
import com.zs.compose.foundation.unit.calculateLeftPaddingInset
import com.zs.compose.foundation.unit.calculateRightInset
import com.zs.compose.foundation.unit.calculateTopInset

private const val TAG = "DecoratorNode"

// Custom Modifier.Node that handles background, foreground, border, elevation,
// scaling, noise effects, and content padding in a single unified draw/layout pass.
internal class DecoratorNode(
    // 🎨 Background styling
    var backgroundColor: Color,          // Solid background color
    var backgroundBrush: Brush?,         // Gradient/pattern background
    var backgroundAlpha: Float,          // Transparency for background

    // 🎨 Foreground styling (overlay drawn above content)
    var foregroundColor: Color,
    var foregroundBrush: Brush?,
    var foregroundAlpha: Float,

    // 🔲 Shape definition (used for outline, clipping, border)
    var shape: Shape,

    // ⬛ Border styling
    var borderColor: Color,
    var borderBrush: Brush?,
    var borderWidth: Dp,
    var borderPathEffect: PathEffect? = null, // e.g. dashed border

    // Outline styling (for styling that strictly traces the outer contour of the content.)
    var outlineColor: Color,
    var outlineGap: Dp,
    var outlineBrush: Brush?,
    var outlineWidth: Dp,
    var outlinePathEffect: PathEffect? = null, // e.g. dashed outine

    // 🌑 Elevation (shadow depth)
    var elevation: Dp,

    // 🔍 Transformations
    var scaleX: Float,
    var scaleY: Float,

    // 🎛️ Noise effect (grain overlay)
    var roughness: Float,

    // 📐 Content padding (space inside border before child content)
    var edgeInsets: EdgeInsets
) : Modifier.Node(), DrawModifierNode, LayoutModifierNode, ObserverModifierNode,
    SemanticsModifierNode, CompositionLocalConsumerModifierNode {
    // Disable auto-invalidation for performance; we manually control redraws
    // This node doesn’t affect parent bounds (important for hit-testing/layout)
    override val shouldAutoInvalidate = false
    override val isImportantForBounds = false

    // GraphicsLayer used for clipping, shadow, scaling, and recording draw ops
    private lateinit var graphicsLayer: GraphicsLayer

    // 🗂️ Cached outline state to avoid recomputation unless inputs change
    private var lastSize: Size = Size.Unspecified
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null
    private var lastShape: Shape? = null
    private var tmpOutline: Outline? = null

    // Cached shader brush for noise effect
    // Cached border stroke style
    var noiseEffectBrush: ShaderBrush? = null
    var borderStroke: Stroke? = null
    var outlineStroke: Stroke? = null

    // 🔗 Lifecycle: initialize graphics layer when node is attached
    override fun onAttach() {
        if (!(::graphicsLayer.isInitialized) || graphicsLayer.isReleased) graphicsLayer =
            requireGraphicsContext().createGraphicsLayer()
    }

    // 📏 Measure phase: apply content padding and enforce constraints
    override fun MeasureScope.measure(
        measurable: Measurable, constraints: Constraints
    ): MeasureResult {
        // Resolve padding based on layout direction
        val leftPadding = edgeInsets.calculateLeftPaddingInset(layoutDirection)
        val topPadding = edgeInsets.calculateTopInset()
        val rightPadding = edgeInsets.calculateRightInset(layoutDirection)
        val bottomPadding = edgeInsets.calculateBottomInset()

        // Ensure padding is valid
        requirePrecondition((leftPadding >= 0.dp) && (topPadding >= 0.dp) && (rightPadding >= 0.dp) && (bottomPadding >= 0.dp)) {
            "Padding must be non-negative"
        }

        // Convert to pixels
        val roundedLeftPadding = leftPadding.roundToPx()
        val horizontal = roundedLeftPadding + rightPadding.roundToPx()
        val roundedTopPadding = topPadding.roundToPx()
        val vertical = roundedTopPadding + bottomPadding.roundToPx()

        // Measure child with reduced constraints (accounting for padding)
        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))

        // Expand measured size to include padding
        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)

        // Place child inside padded area
        return layout(width, height) { placeable.place(roundedLeftPadding, roundedTopPadding) }
    }

    // ♿ Accessibility semantics: expose shape
    override fun SemanticsPropertyReceiver.applySemantics() {
        this.shape = this@DecoratorNode.shape
    }

    // 🔄 Reset cached outline when observed reads change
    override fun onObservedReadsChanged() {
        lastSize = Size.Unspecified
        lastLayoutDirection = null
        lastOutline = null
        lastShape = null
        invalidateDraw() // force redraw with updated cache
    }

    // 🖼️ Outline calculation with caching
    private fun ContentDrawScope.obtainOutline(): Outline {
        val outline: Outline?
        if (size == lastSize && layoutDirection == lastLayoutDirection && lastShape == shape) {
            outline = lastOutline!!
        } else {
            // Manually observe reads so we can directly invalidate the outline when it changes
            // Use tmpOutline to avoid creating an object reference to local var outline
            observeReads { tmpOutline = shape.createOutline(size, layoutDirection, this) }
            outline = tmpOutline
            tmpOutline = null
        }
        lastOutline = outline
        lastSize = size
        lastLayoutDirection = layoutDirection
        lastShape = shape
        return outline!!
    }

    // 🎨 Draw phase: background → content → noise → foreground → border
    override fun ContentDrawScope.draw() {
        val outline = obtainOutline()
        val borderWidthPx = if (borderWidth.isSpecified) borderWidth.toPx() else -1f

        // Draws a silhouette-style outline positioned at a configurable distance (GAP)
        // from the actual content. The GAP ensures the outline sits outside the shape
        // rather than overlapping it.
        //
        // Currently, we only support drawing outlines from paths, which may represent
        // complex shapes (e.g., a compact disk with a hole in the center). For now, we
        // assume the background covers any internal holes; in future we may revisit
        // this with more precise operations.
        //
        // TODO: Explore better alternatives such as clipPath or PathOperation.Difference
        // for handling complex shapes and internal cutouts.
        //
        // Note: This outline is drawn here instead of using a graphicsLayer to avoid
        // unintended clipping behavior.
        val outlineWidthPx = outlineWidth.toPx().coerceAtLeast(0f)
        val outlineGapPx = outlineGap.toPx().coerceAtLeast(0f)
        if (outlineWidthPx >= 0 && (outlineColor.isSpecified || outlineBrush != null)) {
            // Compute scale factors relative to the content size.
            // The outline is centered, so we expand by GAP and half the outline width
            // to ensure the stroke sits correctly outside the content.
            val scaleOutlineX = (size.width + outlineGapPx * 2 + outlineWidthPx / 2) / size.width
            val scaleOutlineY = (size.height + outlineGapPx * 2 + outlineWidthPx / 2) / size.height

            scale(scaleX = scaleOutlineX, scaleY = scaleOutlineY) {
                val stroke = outlineStroke ?: Stroke(outlineWidthPx, pathEffect = outlinePathEffect)
                outlineStroke = stroke
                if (outlineColor.isSpecified)
                    drawOutline(outline, color = outlineColor, style = stroke)
                val brush = outlineBrush ?: return@scale
                drawOutline(outline, brush = brush, style = stroke)
            }
        }

        // Configure graphics layer
        graphicsLayer.setOutline(outline)
        graphicsLayer.shadowElevation = elevation.toPx()
        graphicsLayer.clip = true
        graphicsLayer.scaleX = scaleX
        graphicsLayer.scaleY = scaleY

        // Record drawing commands into graphics layer
        graphicsLayer.record {
            // Draw Background
            // Backgrounds are drawn first, behind the content.
            if (backgroundColor != Color.Unspecified) drawRect(
                backgroundColor,
                alpha = backgroundAlpha
            )
            val backgroundBrush = backgroundBrush
            if (backgroundBrush != null) drawRect(backgroundBrush, alpha = backgroundAlpha)
            // B. Draw Main Content
            // This calls the actual Composable content that this modifier is attached to.
            this@draw.drawContent()
            // Apply Texture Effects
            // If noise is enabled, we overlay the noise texture using the Overlay blend mode.
            // This adds fine-grained grain/texture to both the background and the content,
            // increasing visual depth without relying on high-resolution assets.
            val noise =
                if (noiseEffectBrush == null && roughness > 0) ShaderBrush(id = R.drawable.noise) else noiseEffectBrush
            noiseEffectBrush = noise // cache
            if (noise != null) drawRect(noise, alpha = roughness, blendMode = BlendMode.Overlay)

            // Draw Foreground
            // Foregrounds (overlays) are drawn on top of the content, but inside the border.
            if (foregroundColor != Color.Unspecified) drawRect(
                foregroundColor,
                alpha = foregroundAlpha
            )
            val foregroundBrush = foregroundBrush
            if (foregroundBrush != null) drawRect(foregroundBrush, alpha = foregroundAlpha)

            // Draw Border/Outline
            // We draw the border last. We use a Stroke style with double thickness (borderWidthPx * 2)
            // because drawOutline centered on the path clips half of the stroke width inside/outside.
            if (borderWidthPx != -1f && (borderColor.isSpecified || borderBrush != null)) {
                val style = borderStroke ?: Stroke(borderWidthPx * 2, pathEffect = borderPathEffect)
                borderStroke = style // cache
                if (borderColor.isSpecified) drawOutline(
                    outline,
                    style = style,
                    color = borderColor
                )
                val brush = borderBrush
                if (brush != null) drawOutline(outline, style = style, brush = brush)
            }


        }
        drawLayer(graphicsLayer)
    }

    override fun onDetach() {
        super.onDetach()
        if (!(::graphicsLayer.isInitialized) || graphicsLayer.isReleased) graphicsLayer =
            requireGraphicsContext().createGraphicsLayer()
        noiseEffectBrush = null
        borderStroke = null
    }
}