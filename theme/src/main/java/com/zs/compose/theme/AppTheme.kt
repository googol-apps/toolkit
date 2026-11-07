@file:OptIn(ExperimentalFoundationStyleApi::class)

package com.zs.compose.theme

import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


object AppTheme {
    /**
     * Retrieves the current [Colors] at the call site's position in the hierarchy.
     */
    val colors: Colors
        @Composable @ReadOnlyComposable get() = LocalColors.current

    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current

    @Composable
    @NonRestartableComposable
    operator fun invoke(
        colors: Colors = lightColors(),
        shapes: Shapes = Shapes(),
        content: @Composable () -> Unit
    ) = CompositionLocalProvider(LocalColors provides colors, content = content)
}