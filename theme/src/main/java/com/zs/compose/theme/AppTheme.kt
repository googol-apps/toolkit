/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 26-01-2025.
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

@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalThemeApi::class)

package com.zs.compose.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionDefaults
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize
import androidx.compose.animation.SharedTransitionScope.PlaceholderSize.Companion.ContentSize
import androidx.compose.animation.SharedTransitionScope.ResizeMode
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.zs.compose.foundation.OrientRed
import com.zs.compose.foundation.SepiaBrown
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.TrafficYellow
import com.zs.compose.foundation.UmbraGrey
import com.zs.compose.theme.AppTheme.invoke
import com.zs.compose.theme.MotionScheme.Companion.standard
import com.zs.compose.theme.text.ProvideTextStyle


/**
 * Retrieves a dynamic accent color based on the device's API level and the current theme (light or dark).
 *
 * On Android 14 (API level 34) and above, it uses the system's primary light or dark color.
 * On older devices, it uses system_accent1_600 for light themes and system_accent1_200 for dark themes.
 *
 * @param context The application context.
 * @param darkTheme `true` if the current theme is dark, `false` if it's light.
 * @return A [Color] object representing the dynamic accent color.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun dynamicAccentColor(context: Context, darkTheme: Boolean): Color {
    val res = context.resources
    val color = when {
        Build.VERSION.SDK_INT >= 34 && !darkTheme -> res.getColor(
            android.R.color.system_primary_light,
            context.theme
        )

        Build.VERSION.SDK_INT >= 34 && darkTheme -> res.getColor(
            android.R.color.system_primary_dark,
            context.theme
        )

        !darkTheme -> res.getColor(
            android.R.color.system_accent1_600,
            context.theme
        )// light, tonalPalette.primary40, // dark tonalPalette.primary80
        else -> res.getColor(android.R.color.system_accent1_200, context.theme)
    }
    return Color(color)
}

// source: https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MaterialTheme.kt;bpv=0
// commit date: 2024-09-26 23:43
@Composable
/*@VisibleForTesting*/
internal fun rememberTextSelectionColors(colorScheme: Colors): TextSelectionColors {
    val primaryColor = colorScheme.accent
    return remember(primaryColor) {
        TextSelectionColors(
            handleColor = primaryColor,
            backgroundColor = primaryColor.copy(alpha = TextSelectionBackgroundOpacity),
        )
    }
}

/*@VisibleForTesting*/
internal const val TextSelectionBackgroundOpacity = 0.4f

/**
 * Provides a [CompositionLocal] to access the current [SharedTransitionScope].
 *
 * This CompositionLocal should be provided bya parent composable that manages shared transitions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
internal val LocalSharedTransitionScope =
    staticCompositionLocalOf<SharedTransitionScope> {
        error("CompositionLocal LocalSharedTransition not present")
    }

/**
 * A read-only `CompositionLocal` that provides the current [MotionScheme] to Material 3
 * components.
 *
 * The motion scheme is typically supplied by [AppTheme.motionScheme] and can be overridden
 * for specific UI subtrees by wrapping it with another [AppTheme].
 *
 * This API is exposed to allow retrieving motion values from inside
 * `CompositionLocalConsumerModifierNode` implementations, but in most cases it's recommended to
 * read the motion values from [AppTheme.motionScheme].
 */
@Suppress("CompositionLocalNaming")
@ExperimentalThemeApi
internal val LocalMotionScheme =
    staticCompositionLocalOf { standard() }

/**
 * Provides a[CompositionLocal] to access the current [AnimatedVisibilityScope].
 *
 * This CompositionLocal should be provided by a parent composable that manages animated visibility.
 */
val LocalNavAnimatedVisibilityScope =
    staticCompositionLocalOf<AnimatedVisibilityScope> { error("CompositionLocal LocalSharedTransition not present") }

/**
 * Controls whether shared element transitions are enabled.
 */
val LocalSharedAnimsEnabled = staticCompositionLocalOf { true }

/**
 * Contains functions to access the current theme values provided at the call site's position in the
 * hierarchy.
 */
object AppTheme {
    /**
     * Retrieves the current [Colors] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeColorSample
     */
    val colors: Colors
        @Composable @ReadOnlyComposable get() = LocalColors.current

    /**
     * Retrieves the current [Typography] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeTextStyleSample
     */
    val typography: Typography
        @Composable @ReadOnlyComposable get() = LocalTypography.current

    /**
     * Retrieves the current [Shapes] at the call site's position in the hierarchy.
     *
     * @sample androidx.compose.material3.samples.ThemeShapeSample
     */
    val shapes: Shapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current

    /** Retrieves the current [SharedTransitionScope] at the call site's position in the hierarchy. */
    @OptIn(ExperimentalSharedTransitionApi::class)
    val sharedTransitionScope
        @Composable
        @ReadOnlyComposable
        get() = LocalSharedTransitionScope.current

    /**
     *
     * [https://m3.material.io/styles/motion/overview/how-it-works#17740687-82eb-4be1-9697-1363643f792c](Docs)
     * @see MotionScheme
     *
     */
    @ExperimentalThemeApi
    val motionScheme: MotionScheme
        @Composable @ReadOnlyComposable get() = LocalMotionScheme.current

    /**
     * Composable function to set up the application's theme using the provided
     * colors, shapes, motion scheme, and typography. It uses [SharedTransitionLayout] to
     * enable shared element transitions within the content.
     *
     * @param colors The [Colors] to be used in the theme. Defaults to [AppTheme.colors].
     * @param shapes The [Shapes] to be used in the theme. Defaults to [AppTheme.shapes].
     * @param motionScheme The [MotionScheme] to be used for animations. Defaults to [MotionScheme.expressive].
     * @param indication The [Indication] to be used for interactive elements. Defaults to [ripple].
     * @param typography The [Typography] to be used in the theme. Defaults to [AppTheme.typography].
     * @param content The composable content to be displayed within the theme.
     *
     * @see SharedTransitionLayout
     * @see CompositionLocalProvider
     * @see ProvideTextStyle
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    operator fun invoke(
        colors: Colors = AppTheme.colors,
        shapes: Shapes = AppTheme.shapes,
        motionScheme: MotionScheme = MotionScheme.expressive(),
        indication: Indication = ripple(),
        typography: Typography = AppTheme.typography,
        content: @Composable () -> Unit
    ) {
        val selectionColors = rememberTextSelectionColors(colors)
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalColors provides colors,
                LocalIndication provides indication,
                LocalMotionScheme provides motionScheme,
                LocalShapes provides shapes,
                LocalTextSelectionColors provides selectionColors,
                LocalTypography provides typography,
                LocalSharedTransitionScope provides this,
            ) {
                ProvideTextStyle(value = typography.body1, content = content)
            }
        }
    }

    /**
     * Provides a composable function to set up the application's theme using the provided
     * colors, typography, and shapes.
     *
     * @param isLight  if true, applies the light theme.
     * @param fontFamily  the font family to be used in the theme.
     * @param accent  the accent color to be used in the theme.
     * @param indication  the indication to be used in the theme; defaults to [ripple()]
     * @param content  the composable content to be displayed within the theme.
     *
     * @see invoke
     */
    @Composable
    operator fun invoke(
        isLight: Boolean,
        accent: Color = if (!isLight) Color.TrafficYellow else Color.SepiaBrown,
        fontFamily: FontFamily = FontFamily.Default,
        shapes: Shapes = AppTheme.shapes,
        indication: Indication = ripple(),
        motionScheme: MotionScheme = MotionScheme.expressive(),
        content: @Composable () -> Unit
    ) {
        val background by animateColorAsState(
            targetValue = when {
                !isLight -> Color(0xFF0E0E0F)
                else -> lerp(start = Color.White, accent, 0.12f)
            },
            animationSpec = motionScheme.slowEffectsSpec(), label = "background"
        )
        val primary by animateColorAsState(accent, motionScheme.defaultEffectsSpec(), "accent")
        val colors = Colors(
            accent = primary,
            background = background,
            onBackground = if (isLight) Color.UmbraGrey else Color.SignalWhite,
            onAccent = if (primary.luminance() > 0.45f) Color.Black else Color.SignalWhite,
            error = Color.OrientRed,
            onError = Color.SignalWhite,
        )

        invoke(
            colors = colors,
            typography = Typography(defaultFontFamily = fontFamily),
            shapes = shapes,
            indication = indication,
            motionScheme = motionScheme,
            content = content
        )
    }
}

/**
 * @param renderInOverlay pass null to make this fun handle with default strategy.
 * @see androidx.compose.animation.SharedTransitionScope.renderInSharedTransitionScopeOverlay
 */
fun Modifier.renderInSharedTransitionScopeOverlay(
    zIndexInOverlay: Float = 0f,
    renderInOverlay: (() -> Boolean)? = null ,
) = composed {
    // TODO - Find new way to avoid using composed modifier.
    // Retrieve the current SharedTransitionScope from the composition local.
    val sharedTransitionScope = LocalSharedTransitionScope.current
    // Apply the original modifier from the scope.
    // If renderInOverlay is not provided, use the default behavior of rendering when a transition is active.
    with(sharedTransitionScope) {
        renderInSharedTransitionScopeOverlay(
            zIndexInOverlay = zIndexInOverlay,
            renderInOverlay = renderInOverlay ?: { isTransitionActive },
        )
    }
}


/**
 * @return the state of shared contnet corresponding to [key].
 * @see androidx.compose.animation.SharedTransitionScope.rememberSharedContentState
 */
@Composable
private inline fun rememberSharedContentState(key: Any) =
    with(AppTheme.sharedTransitionScope) {
        rememberSharedContentState(key = key)
    }


private val ParentClip: OverlayClip =
    object : OverlayClip {
        override fun getClipPath(
            sharedContentState: SharedContentState,
            bounds: Rect,
            layoutDirection: LayoutDirection,
            density: Density,
        ): Path? {
            return sharedContentState.parentSharedContentState?.clipPathInOverlay
        }
    }

/**
 * A shared bounds modifier that uses scope from [AppTheme]'s [AppTheme.sharedTransitionScope] and
 * [AnimatedVisibilityScope] from [LocalNavAnimatedVisibilityScope]
 * @see androidx.compose.animation.SharedTransitionScope.sharedBounds
 */
fun Modifier.sharedBounds(
    key: Any,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    boundsTransform: BoundsTransform = SharedTransitionDefaults.BoundsTransform,
    resizeMode: ResizeMode = scaleToBounds(ContentScale.FillWidth, Center),
    placeholderSize: PlaceholderSize = ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip,
) = composed {
    val enabled = LocalSharedAnimsEnabled.current
    if (!enabled) return@composed Modifier
    val navAnimatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val sharedContentState = rememberSharedContentState(key)
    with(sharedTransitionScope) {
        Modifier.sharedBounds(
            sharedContentState = sharedContentState,
            animatedVisibilityScope = navAnimatedVisibilityScope,
            enter = enter,
            exit = exit,
            boundsTransform = boundsTransform,
            resizeMode = resizeMode,
            placeholderSize = placeholderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition
        )
    }
}

/**
 * A shared Element modifier that uses scope from [AppTheme]'s [AppTheme.sharedTransitionScope] and
 * [AnimatedVisibilityScope] from [LocalNavAnimatedVisibilityScope]
 * @see androidx.compose.animation.SharedTransitionScope.sharedElement
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedElement(
    key: Any,
    boundsTransform: BoundsTransform = SharedTransitionDefaults.BoundsTransform,
    placeholderSize: PlaceholderSize = ContentSize,
    renderInOverlayDuringTransition: Boolean = true,
    zIndexInOverlay: Float = 0f,
    clipInOverlayDuringTransition: OverlayClip = ParentClip,
) = composed {
    val enabled = LocalSharedAnimsEnabled.current
    if (!enabled) return@composed Modifier
    val sharedContentState = rememberSharedContentState(key = key)
    val navAnimatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    with(sharedTransitionScope) {
        sharedElement(
            sharedContentState = sharedContentState,
            placeholderSize = placeholderSize,
            renderInOverlayDuringTransition = renderInOverlayDuringTransition,
            zIndexInOverlay = zIndexInOverlay,
            animatedVisibilityScope = navAnimatedVisibilityScope,
            boundsTransform = boundsTransform,
            clipInOverlayDuringTransition = clipInOverlayDuringTransition
        )
    }
}