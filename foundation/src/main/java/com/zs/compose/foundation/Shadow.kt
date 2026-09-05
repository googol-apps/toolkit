/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 30-05-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.compose.foundation

import android.graphics.BlurMaskFilter
import android.util.Log
import androidx.annotation.IntRange
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.absoluteValue

private inline val Offset.mirror get() = copy(-x, -y)

private const val POINT_60 = 0.6f
private const val POINT_95 = 0.95f

/**
 * Calculates the shadow offset from the center based on a fixed angle direction.
 *
 * @param angle The direction of the light source in degrees (0, 45, 90, 135, 180, 225, 270, 315).
 * @param value The shadow offset magnitude in pixels.
 * @param elevated Whether the shadow should appear elevated or sunken.
 */
private fun toOffset(angle: Int, value: Float, elevated: Boolean = true): Offset {
    return when (angle % 360) {
        270/*TOP*/ -> if (elevated) Offset(0f, -value) else Offset(0f, value)
        90/*BOTTOM*/ -> if (elevated) Offset(0f, value) else Offset(0f, -value)
        180/*"LEFT"*/ -> if (elevated) Offset(-value, 0f) else Offset(value, 0f)
        0/*RIGHT*/ -> if (elevated) Offset(value, 0f) else Offset(-value, 0f)
        225/*TOP_LEFT*/ -> if (elevated) Offset(-value, -value) else Offset(value, value)
        135 /*BOTTOM_LEFT*/ -> if (elevated) Offset(-value, value) else Offset(value, -value)
        315/*TOP_RIGHT*/ -> if (elevated) Offset(value, -value) else Offset(-value, value)
        45 /*BOTTOM_RIGHT*/ -> if (elevated) Offset(value, value) else Offset(-value, -value)
        else -> error("Invalid angle")
    }
}

// This helper function draws a shadow using the provided size, path, and paint.
//
// If the scale is greater than 0:
//   - If 'stroke' is not NaN:
//     - We clip the canvas and then scale it.
//     - This is useful for drawing shadows that overlay content because the stroke
//       is transparent but has an outline.
//   - If 'stroke' is NaN:
//     - We draw the shadow layer as a non-stroked layer, as it's intended to be
//       covered by the shadow itself.
//
// TODO: Investigate why 'stroke' cannot be used consistently in both scenarios.

/**
 * Draws shadow layer on Canvas.
 */
private fun Canvas.shadow(
    offset: Offset, size: Size, path: Path, paint: Paint, stroke: Float = Float.NaN
) {
    val drawFill = stroke.isNaN()
    val canvas = this
    canvas.save()
    // in case foreground clip it.
    if (!drawFill) {
        canvas.clipPath(path)
        val (width, height) = size
        val scaleX = (width + 2 * stroke) / width
        val scaleY = (height + 2 * stroke) / height
        val scaleF = stroke
        scale(scaleX, scaleY)
        canvas.translate(-scaleF, -scaleF)
    }
    canvas.translate(offset.x, offset.y)
    canvas.drawPath(path, paint)
    canvas.restore()
}

/**
 * Applies a shadow effect to the composable.
 *
 * This modifier allows for creating both elevated (raised) and sunken (pressed) shadow effects.
 *
 * @param elevation The depth of the shadow. A positive value creates an elevated shadow,
 * while a negative value creates a sunken shadow.
 * @param lightShadowColor The color of the lighter part of the shadow, typically positioned
 * towards the light source. Defaults to [Color.White].
 * @param darkShadowColor The color of the darker part of the shadow, typically positioned
 * away from the light source. Defaults to [Color.Gray].
 * @param lightSourceAt The angle of the light source in degrees, in multiples of 45.
 *  - 0 degrees: light from the right.
 *  - 45 degrees: light from the bottom-right.
 * @param shape The shape of the shadow. Defaults to [CircleShape].
 * Currently, this modifier best supports complete shapes. Complex shapes with cutouts
 * (e.g., a disk with a circle punched in the center) might not render as expected.
 */
@Deprecated("Compose introduced its own modifiers for such work.")
@ExperimentalFoundationApi
fun Modifier.shadow(
    elevation: Dp, // Elevation: negative for sunken, positive for elevated.
    lightShadowColor: Color = Color.White, // Color of the lighter shadow, typically towards the light source.
    darkShadowColor: Color = Color.Gray,   // Color of the darker shadow, typically away from the light source.
    lightSourceAt: Int = 225, // Angle of the light source in multiples of 45 degrees. 0° is right, 45° is bottom-right, etc.
    shape: Shape = CircleShape // Shape of the shadow. Currently best supports complete shapes (e.g., not shapes with holes like a disk with a punched-out center).
) = if (elevation == 0.dp) this else this then ShadowElement(
    lightShadowColor = lightShadowColor,
    darkShadowColor = darkShadowColor,
    lightSourceAt = lightSourceAt,
    shape = shape,
    elevation = elevation
)

private data class ShadowElement(
    val lightShadowColor: Color,
    val darkShadowColor: Color,
    val lightSourceAt: Int,
    val shape: Shape,
    val elevation: Dp
) : ModifierNodeElement<ShadowNode>() {
    override fun create(): ShadowNode = ShadowNode(
        lightShadowColor = lightShadowColor,
        darkShadowColor = darkShadowColor,
        lightSourceAt = lightSourceAt,
        shape = shape,
        elevation = elevation
    )

    override fun update(node: ShadowNode) {
        node.lightShadowColor = lightShadowColor
        node.darkShadowColor = darkShadowColor
        node.lightSourceAt = lightSourceAt
        if (shape != node.shape) {
            node.shape = shape
            node.outline = null
        }
        if (elevation != node.elevation) {
            node.blurMaskFilter = null
        }
        Log.d("Ambient Shadow", "updateing ")
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "ambientShadow"
        properties["lightShadowColor"] = lightShadowColor
        properties["darkShadowColor"] = darkShadowColor
        properties["angle"] = lightSourceAt
        properties["shape"] = shape
    }
}


private class ShadowNode(
    var lightShadowColor: Color,
    var darkShadowColor: Color,
    @IntRange(0, 359) var lightSourceAt: Int,
    var shape: Shape,
    var elevation: Dp,
) : Modifier.Node(), DrawModifierNode {
    // Disable auto-invalidation for this node.
    override val shouldAutoInvalidate: Boolean = false

    // cached values meant to be recreated once null.
    private val paint: Paint = Paint()
    var outline: Path? = null
    var blurMaskFilter: BlurMaskFilter? = null

    override fun ContentDrawScope.draw() {
        // Calculate the elevation in pixels and whether the element is elevated.
        val elevationPx = elevation.toPx().absoluteValue
        val elevated = elevation > 0.dp

        // Determine the magnitude of the shadow based on elevation.
        val magnitude = elevationPx * if (elevated) POINT_60 else POINT_95

        if (blurMaskFilter == null) {
            // Create a blur mask filter for the shadow.
            val mask = BlurMaskFilter(
                (elevationPx * if (elevated) POINT_95 else POINT_60), BlurMaskFilter.Blur.NORMAL
            )
            val native = paint.asFrameworkPaint()
            native.maskFilter = mask
            // make paint for each shadow type separately
            when (elevated) {
                // If elevated, enable dithering for a smoother shadow.
                true -> native.isDither = true
                else -> {
                    native.strokeWidth = magnitude
                    native.style = android.graphics.Paint.Style.STROKE
                }
            }
            blurMaskFilter = mask
        }

        // Create or retrieve the outline path for the shadow.
        val outline = outline ?: Path().also {
            it.addOutline(shape.createOutline(size, layoutDirection, this@draw))
            this@ShadowNode.outline = it
        }

        // Calculate the shadow offset based on the light source angle and magnitude.
        val offset = toOffset(lightSourceAt, magnitude, elevated)

        when {
            // If elevated, draw both light and dark shadows.
            elevated -> {
                drawIntoCanvas { canvas ->
                    // Draw the light shadow.
                    paint.color = lightShadowColor
                    canvas.shadow(
                        offset = offset, path = outline, paint = paint, size = size
                    )

                    // Mirror the offset for the dark shadow.
                    val mirrored = offset.mirror
                    // Draw the dark shadow.
                    paint.color = darkShadowColor
                    canvas.shadow(
                        offset = mirrored, path = outline, paint = paint, size = size
                    )
                    // Clip the canvas to the outline to ensure the shadow doesn't extend beyond the shape.
                    canvas.clipPath(outline)
                }
                // Draw the content on top of the shadows.
                drawContent()
            }

            // If not elevated (sunken), draw the content first, then the shadows.
            else -> {
                // Draw the content.
                drawContent()

                drawIntoCanvas { canvas ->
                    // Draw the light shadow (mirrored offset for sunken effect).
                    val mirror = offset.mirror
                    paint.color = lightShadowColor
                    canvas.shadow(
                        offset = mirror,
                        paint = paint,
                        path = outline,
                        stroke = magnitude,
                        size = size
                    )

                    // Draw the dark shadow.
                    paint.color = darkShadowColor
                    canvas.shadow(
                        offset = offset,
                        paint = paint,
                        path = outline,
                        stroke = magnitude,
                        size = size
                    )
                    // Optional: Clip the canvas to the path if needed for specific effects.
                }

            }
        }
    }
}