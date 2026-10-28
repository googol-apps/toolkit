package com.zs.compose.foundation.style

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalContext
import kotlin.system.measureNanoTime

private const val TAG = "DecoratorUtil"

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
context(node: CompositionLocalConsumerModifierNode)
internal fun ShaderBrush(@DrawableRes id: Int): ShaderBrush {
    // Determine the appropriate image source based on the ImageBrush type
    // Decode the resource into a Bitmap for resource-based ImageBrushes.
    val resources = node.currentValueOf(LocalContext).resources
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
    val shader = ImageShader(img, Repeated, Repeated)
    return ShaderBrush(shader)
}

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
    val shader = ImageShader(img, Repeated, Repeated)
    return ShaderBrush(shader)
}
