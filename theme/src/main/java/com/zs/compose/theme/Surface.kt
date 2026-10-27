/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 27-01-2025.
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

package com.zs.compose.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.isContainer
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.Slot
import com.zs.compose.foundation.style.styleable

@PublishedApi
internal val BlockInputModifier = Modifier.pointerInput(Unit, {})
@PublishedApi
internal val SizeHackModifier = Modifier.defaultMinSize(0.dp)

/**
 * A central piece of the UI hierarchy that provides a background color, content color, and
 * elevation to its content. It also handles common tasks like clipping to a shape and
 * blocking pointer input from leaking through to the background.
 *
 * @param modifier The [Modifier] to be applied to this surface.
 * @param shape The [Shape] of the surface and its shadow.
 * @param color The background color of the surface.
 * @param contentColor The preferred color for content within this surface, used as the
 * default [LocalContentColor] for child composables.
 * @param border Optional [BorderStroke] to be drawn around the edge of the surface.
 * @param elevation The thickness of the shadow drawn below the surface.
 * @param content The composable content to be displayed inside the surface.
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContentColor provides contentColor
    ) {
        Slot(
            content = content,
            modifier = modifier
                .styleable(
                    shape = shape,
                    backgroundColor = color,
                    elevation = elevation,
                    border = border,
                )
                .semantics(mergeDescendants = false) {
                    @Suppress("DEPRECATION")
                    isContainer = true
                }
                // Workaround: Force an initial size of 0.dp to prevent the Slot/decorator from
                // defaulting to an incorrect size (e.g., 4.dp) irrespective of explicit size provided
                // via the incoming modifier.
                // TODO: Investigate the root cause in Slot/decorator and remove this hack.
                .then(SizeHackModifier)
                .then(BlockInputModifier)
        )
    }
}

/**
 * Clickable variant of [Surface].
 *
 * This version of Surface provides a clickable interaction, making it suitable for
 * components like cards or list items that should respond to user taps.
 *
 * @param onClick Callback to be invoked when the surface is clicked.
 * @param modifier The [Modifier] to be applied to this surface.
 * @param enabled Controls the enabled state of the surface. When `false`, this component will
 * not respond to user input, and it will appear visually disabled.
 * @param shape The [Shape] of the surface and its shadow.
 * @param color The background color of the surface.
 * @param contentColor The preferred color for content within this surface.
 * @param border Optional [BorderStroke] to be drawn around the edge of the surface.
 * @param elevation The thickness of the shadow drawn below the surface.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * interactions for this surface.
 * @param indication [Indication] to be shown when surface is pressed.
 * @param content The composable content to be displayed inside the surface.
 */
@ExperimentalThemeApi
@Composable
inline fun Surface(
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
    crossinline content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Slot(
            modifier = modifier
                .styleable(
                    shape = shape,
                    backgroundColor = color,
                    border = border,
                    elevation = elevation
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onClick = onClick
                )
                .minimumInteractiveComponentSize(),
            content = content
        )
    }
}

/**
 * Selectable variant of [Surface].
 *
 * This version of Surface provides a selectable interaction, making it suitable for
 * components like tabs or radio buttons that exist within a selection group.
 *
 * @param selected Whether this surface is currently selected.
 * @param onClick Callback to be invoked when the surface is clicked.
 * @param modifier The [Modifier] to be applied to this surface.
 * @param enabled Controls the enabled state of the surface. When `false`, this component will
 * not respond to user input, and it will appear visually disabled.
 * @param shape The [Shape] of the surface and its shadow.
 * @param color The background color of the surface.
 * @param contentColor The preferred color for content within this surface.
 * @param border Optional [BorderStroke] to be drawn around the edge of the surface.
 * @param elevation The thickness of the shadow drawn below the surface.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * interactions for this surface.
 * @param indication [Indication] to be shown when surface is pressed.
 * @param content The composable content to be displayed inside the surface.
 */
@ExperimentalThemeApi
@Composable
inline fun Surface(
    selected: Boolean,
    noinline onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
    crossinline content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Slot(
            modifier = modifier
                .styleable(
                    shape = shape,
                    backgroundColor = color,
                    border = border,
                    elevation = elevation
                )
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onClick = onClick
                )
                .minimumInteractiveComponentSize(),
            content = content
        )
    }
}

/**
 * Toggleable variant of [Surface].
 *
 * This version of Surface provides a toggleable interaction, making it suitable for
 * components like switches, checkboxes, or toggle buttons.
 *
 * @param checked Whether this surface is currently checked/active.
 * @param onCheckedChange Callback to be invoked when the toggle state changes.
 * @param modifier The [Modifier] to be applied to this surface.
 * @param enabled Controls the enabled state of the surface. When `false`, this component will
 * not respond to user input, and it will appear visually disabled.
 * @param shape The [Shape] of the surface and its shadow.
 * @param color The background color of the surface.
 * @param contentColor The preferred color for content within this surface.
 * @param border Optional [BorderStroke] to be drawn around the edge of the surface.
 * @param elevation The thickness of the shadow drawn below the surface.
 * @param interactionSource The [MutableInteractionSource] representing the stream of
 * interactions for this surface.
 * @param indication [Indication] to be shown when surface is pressed.
 * @param content The composable content to be displayed inside the surface.
 */
@ExperimentalThemeApi
@Composable
inline fun Surface(
    checked: Boolean,
    noinline onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = AppTheme.colors.background,
    contentColor: Color = contentColorFor(color),
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
    crossinline content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Slot(
            modifier = modifier
                .styleable(
                    shape = shape,
                    backgroundColor = color,
                    border = border,
                    elevation = elevation
                )
                .toggleable(
                    value = checked,
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onValueChange = onCheckedChange
                )
                .minimumInteractiveComponentSize(),
            content = content
        )
    }
}