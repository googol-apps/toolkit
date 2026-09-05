package com.zs.compose.foundation.backdrop.haze

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.FloatRange
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import com.zs.compose.foundation.backdrop.Backdrop
import com.zs.compose.foundation.decorator.decorator

/**
 * A [ModifierNodeElement] that creates and updates a [HazeEffectNode], applying a frosted
 * "mist" (glassmorphism-style) blur effect to the composable it modifies.
 * @see Modifier.hazeEffect
 */
private class HazeEffectElement(
    val backdrop: Backdrop,
    val config: BlurConfig,
    val vibrancy: Float,
    var luminsity: Float,
    val tint: Color,
    val key: Any? = null
) : ModifierNodeElement<HazeEffectNode>() {

    /**
     * Creates a new [HazeEffectNode] initialized with this element's current parameters.
     */
    override fun create(): HazeEffectNode = HazeEffectNode(
        backdrop = backdrop,
        config = config,
        vibrancy = vibrancy,
        luminsity = luminsity,
        tint = tint
    )

    /**
     * Updates an existing [HazeEffectNode] with this element's current parameters,
     * allowing the node to be reused across recompositions instead of being torn down
     * and recreated.
     */
    override fun update(node: HazeEffectNode) {
        // Cache Invalidation:
        // If the blur profile (radius) or vibrancy or luminsity changed, we nullify
        // the cached RenderEffect and ColorMatrix inside the node so they are regenerated on
        // the next draw pass. This avoids recreating expensive GPU objects if the values
        // haven't actually changed.
        if (node.config.radiusPx != config.radiusPx) node.effect = null
        if (node.vibrancy != vibrancy || node.luminsity != luminsity) node.filter = null

        // Apply new values
        node.backdrop = backdrop
        node.config = config
        node.vibrancy = vibrancy
        node.tint = tint

        // Trigger a redraw cycle:
        // Because this modifier manipulates pixels on the screen, changing its properties
        // must explicitly flag the node's layout for a redraw.
        node.invalidateDraw()
    }

    /**
     * Exposes this modifier's parameters to tooling (e.g. the Layout Inspector) under the
     * name `HazeEffect`. This has no impact on runtime performance or logic.
     */
    override fun InspectorInfo.inspectableProperties() {
        name = "HazeEffect"
        properties["backdrop"] = backdrop
        properties["blurConfig"] = config
        properties["vibrancy"] = vibrancy
        properties["tint"] = tint
        properties["luminosity"] = luminsity
        properties["key"] = key
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HazeEffectElement

        if (vibrancy != other.vibrancy) return false
        if (backdrop != other.backdrop) return false
        if (config != other.config) return false
        if (tint != other.tint) return false
        if (luminsity != other.luminsity) return false
        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vibrancy.hashCode()
        result = 31 * result + backdrop.hashCode()
        result = 31 * result + config.hashCode()
        result = 31 * result + tint.hashCode()
        result = 31 * result + luminsity.hashCode()
        result = 31 * result + (key?.hashCode() ?: 0)
        return result
    }
}

/**
 * Applies a frosted glass (haze) effect to the modified composable.
 *
 * The effect combines the provided [backdrop] with a configurable blur,
 * surface color, optional tint, noise, edge highlight, and shape.
 *
 * @param backdrop The [Backdrop] containing the captured UI layer behind this component.
 * @param surface The base color drawn beneath the haze effect.
 * @param blurConfig The blur configuration, including downsample factor and blur radius.
 *                   Defaults to a 0.5f downsample factor and 10f radius.
 * @param tint An optional color overlay applied to the blurred backdrop.
 * @param elevation The elevation applied to the surface.
 * @param vibrancy Controls the saturation of the blurred backdrop. `1f` leaves
 *                 the saturation unchanged.
 * @param luminosity Controls the luminosity adjustment of the blurred backdrop.
 *                   Defaults to the luminance of [tint]. Use `-1f` to disable
 *                   the luminosity adjustment.
 * @param noiseAmount Controls the intensity of the noise/grain applied to the surface.
 * @param edgeHighlight An optional border used to highlight the edge of the glass surface.
 * @param shape The shape of the glass surface.
 * @param key An optional key to force a modifier update. Useful when properties of static backdrops
 *            (like ImageBackdrop or ScreenBackdrop) change, ensuring the modifier element is
 *            re-evaluated.
 *
 * @see Modifier.legacyHazeEffect
 * TODO: Consider refactoring away from the decorator. Currently, noise is drawn over the content;
 *       integrating surface and noise rendering directly into the haze effect would likely
 *       improve performance and visual accuracy.
 */
@RequiresApi(Build.VERSION_CODES.S)
@Stable
@ExperimentalFoundationApi
fun Modifier.hazeEffect(
    backdrop: Backdrop,
    surface: Color,
    blurConfig: BlurConfig = BlurConfig(0.5f, 10.0f),
    tint: Color = Color.Unspecified,
    elevation: Dp = Dp.Unspecified,
    vibrancy: Float = 1.0f,
    @SuppressLint("Range") @FloatRange(0.0, 1.0) luminosity: Float = if (tint.isSpecified)tint.luminance() else -1f,
    @FloatRange(0.0, 1.0) noiseAmount: Float = 0f,
    edgeHighlight: BorderStroke? = null,
    shape: Shape = RectangleShape,
    key: Any? = null
) = this then
        // Draw the base surface and decorative elements such as noise and border.
        Modifier.decorator(
            backgroundColor = surface,
            noiseAlpha = noiseAmount,
            border = edgeHighlight,
            elevation = elevation,
            shape = shape
        ) then
        // Capture and process the backdrop using the configured GPU blur effect.
        HazeEffectElement(
            backdrop = backdrop,
            config = blurConfig,
            luminsity = luminosity,
            vibrancy = vibrancy,
            tint = tint,
            key = key
        )