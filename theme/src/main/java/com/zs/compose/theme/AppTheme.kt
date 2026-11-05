package com.zs.compose.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf


val LocalColors = staticCompositionLocalOf<Colors> { Colors() }



object AppTheme {
    /**
     * Retrieves the current [Colors] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeColorSample
     */
    val colors: Colors
        @Composable @ReadOnlyComposable get() = LocalColors.current

}