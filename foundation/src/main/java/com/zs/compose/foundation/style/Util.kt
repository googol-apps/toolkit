package com.zs.compose.foundation.style

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.style.Breadth
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.foundation.style.ProvidableStyleProperty
import androidx.compose.foundation.style.StyleResolver
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import kotlin.system.measureNanoTime

private const val TAG = "DecoratorUtil"


@OptIn(ExperimentalFoundationStyleApi::class)
private val NoOpMutableStyleState = MutableStyleState(null)

/**
 * A no-op [MutableStyleState] that performs no styling changes.
 *
 * Useful as a default or fallback style state when no styling behaviour is required.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
companion val MutableStyleState.none
    get() = NoOpMutableStyleState

@OptIn(ExperimentalFoundationStyleApi::class)
internal operator fun <T> StyleResolver.get(property: ProvidableStyleProperty<T>): T =
    resolve { property.value }

/**
 * Creates a [ShaderBrush] using a bitmap resource identified by the given drawable ID.
 *
 * This function decodes the specified drawable resource into a bitmap, converts it
 * to an [androidx.compose.ui.graphics.ImageBitmap], and creates an [ImageShader]
 * with [TileMode.Repeated] for both horizontal and vertical axes.
 *
 * @param id The drawable resource ID to be used as the shader texture.
 * @return A [ShaderBrush] configured to repeat the specified image.
 */
internal fun ShaderBrush(context: Resources, @DrawableRes id: Int): ShaderBrush {
    // Determine the appropriate image source based on the ImageBrush type
    // Decode the resource into a Bitmap for resource-based ImageBrushes.
    val resources = context
    val bmp: Bitmap
    val time = measureNanoTime {
        // Decode the resource, disabling scaling for optimal performance.
        bmp = BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply {
            inScaled = false
//            inTargetDensity = inDensity
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                this.inPreferredConfig = Bitmap.Config.HARDWARE
//            }
        })
    }
    Log.d(TAG, "create: (width=${bmp.width}, height=${bmp.height}, time=$time")
    // Convert the decoded Bitmap to an ImageBitmap for use in the ShaderBrush.
    val img = bmp.asImageBitmap()
    // Create and return a ShaderBrush using the ImageShader for repeating the image:
    // TODO - Experiment with different tileModes.
    val shader = ImageShader(img, TileMode.Repeated,
        TileMode.Repeated
    )
    return ShaderBrush(shader)
}

/**
 * Resolves one axis. The returned Constraints uses only its minWidth/maxWidth
 * fields, as a carrier for this axis's min and max. Nothing is allocated.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
internal fun MeasureScope.resolveAxis(inMin: Int, inMax: Int, minDp: Dp, maxDp: Dp, breadth: Breadth): Constraints {
    var styleMin = 0
    var styleMax = Constraints.Infinity

    if (maxDp.isSpecified) styleMax = maxDp.roundToPx().fastCoerceAtLeast(0)
    if (minDp.isSpecified) styleMin = minDp.roundToPx().fastCoerceIn(0, styleMax)

    // A fixed size pins both bounds, still inside the style min/max.
    if (breadth is Breadth.Distance && breadth.value.isSpecified) {
        styleMin = breadth.value.roundToPx().fastCoerceIn(styleMin, styleMax)
        styleMax = styleMin
    }

    // Clamp into what the parent (minus padding) allows.
    var min = if (styleMin == 0) inMin else styleMin.fastCoerceIn(inMin, inMax)
    var max = if (styleMax == Constraints.Infinity) inMax else styleMax.fastCoerceIn(min, inMax)

    // Fractional size comes from the clamped max, and only when bounded.
    if (breadth is Breadth.Fraction && inMax != Constraints.Infinity) {
        val v = (max * breadth.value).fastRoundToInt().fastCoerceIn(min, max)
        min = v
        max = v
    }

    return Constraints(minWidth = min, maxWidth = max)
}