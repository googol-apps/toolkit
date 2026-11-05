package com.zs.compose.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln


/**
 * Returns this surface color with a tonal [tint] overlay for the given [elevation].
 *
 * Higher elevations get a more opaque tint, so surfaces look raised without shadows.
 * Follows the Material 3 tonal elevation curve.
 */
fun Color.atTonalElevation(tint: Color, elevation: Dp): Color {
    if (elevation == 0.dp) return this
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return tint.copy(alpha = alpha).compositeOver(this)
}