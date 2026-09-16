/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 30-01-2025.
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

@file:OptIn(ExperimentalFoundationApi::class, ExperimentalThemeApi::class)

package com.zs.compose.theme.menu

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.thenIf
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.ExperimentalThemeApi
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.Surface
import com.zs.compose.theme.ripple
import com.zs.compose.theme.text.ProvideTextStyle
import com.zs.compose.theme.text.Text
import kotlin.math.max
import kotlin.math.min

// TODO: Consider to move into public [MenuDefaults]
private val DefaultMenuProperties: PopupProperties = PopupProperties(focusable = true)
// copied from material2 menu

// Size defaults.
private val MenuElevation = 2.dp
internal val MenuVerticalMargin = 48.dp
private val DropdownMenuItemHorizontalPadding = 16.dp
internal val DropdownMenuVerticalPadding = 8.dp
private val DropdownMenuItemDefaultMinWidth = 112.dp
private val DropdownMenuItemDefaultMaxWidth = 280.dp
private val DropdownMenuItemDefaultMinHeight = 40.dp

// Menu open/close animation.
internal const val InTransitionDuration = 120
internal const val OutTransitionDuration = 75

// Menu open/close animation.
internal const val ExpandedScaleTarget = 1f
internal const val ClosedScaleTarget = 0.8f
internal const val ExpandedAlphaTarget = 1f
internal const val ClosedAlphaTarget = 0f

/** Default padding used for [DropdownMenuItem]. */
private val DropdownMenuItemContentPadding =
    PaddingValues(horizontal = DropdownMenuItemHorizontalPadding, vertical = 0.dp)

internal fun calculateTransformOrigin(parentBounds: IntRect, menuBounds: IntRect): TransformOrigin {
    val pivotX =
        when {
            menuBounds.left >= parentBounds.right -> 0f
            menuBounds.right <= parentBounds.left -> 1f
            menuBounds.width == 0 -> 0f
            else -> {
                val intersectionCenter =
                    (max(parentBounds.left, menuBounds.left) +
                            min(parentBounds.right, menuBounds.right)) / 2
                (intersectionCenter - menuBounds.left).toFloat() / menuBounds.width
            }
        }
    val pivotY =
        when {
            menuBounds.top >= parentBounds.bottom -> 0f
            menuBounds.bottom <= parentBounds.top -> 1f
            menuBounds.height == 0 -> 0f
            else -> {
                val intersectionCenter =
                    (max(parentBounds.top, menuBounds.top) +
                            min(parentBounds.bottom, menuBounds.bottom)) / 2
                (intersectionCenter - menuBounds.top).toFloat() / menuBounds.height
            }
        }
    return TransformOrigin(pivotX, pivotY)
}

// Menu positioning.
/** Calculates the position of a Material [DropdownMenu]. */
@Immutable
internal data class DropdownMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density,
    val onPositionCalculated: (IntRect, IntRect) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        // The min margin above and below the menu, relative to the screen.
        val verticalMargin = with(density) { MenuVerticalMargin.roundToPx() }
        // The content offset specified using the dropdown offset parameter.
        val contentOffsetX =
            with(density) {
                contentOffset.x.roundToPx() *
                        (if (layoutDirection == LayoutDirection.Ltr) 1 else -1)
            }
        val contentOffsetY = with(density) { contentOffset.y.roundToPx() }

        // Compute horizontal position.
        val leftToAnchorLeft = anchorBounds.left + contentOffsetX
        val rightToAnchorRight = anchorBounds.right - popupContentSize.width + contentOffsetX
        val rightToWindowRight = windowSize.width - popupContentSize.width
        val leftToWindowLeft = 0
        val x =
            if (layoutDirection == LayoutDirection.Ltr) {
                sequenceOf(
                    leftToAnchorLeft,
                    rightToAnchorRight,
                    // If the anchor gets outside of the window on the left, we want to position
                    // toDisplayLeft for proximity to the anchor. Otherwise, toDisplayRight.
                    if (anchorBounds.left >= 0) rightToWindowRight else leftToWindowLeft
                )
            } else {
                sequenceOf(
                    rightToAnchorRight,
                    leftToAnchorLeft,
                    // If the anchor gets outside of the window on the right, we want to
                    // position
                    // toDisplayRight for proximity to the anchor. Otherwise, toDisplayLeft.
                    if (anchorBounds.right <= windowSize.width) leftToWindowLeft
                    else rightToWindowRight
                )
            }
                .firstOrNull { it >= 0 && it + popupContentSize.width <= windowSize.width }
                ?: rightToAnchorRight

        // Compute vertical position.
        val topToAnchorBottom = maxOf(anchorBounds.bottom + contentOffsetY, verticalMargin)
        val bottomToAnchorTop = anchorBounds.top - popupContentSize.height + contentOffsetY
        val centerToAnchorTop = anchorBounds.top - popupContentSize.height / 2 + contentOffsetY
        val bottomToWindowBottom = windowSize.height - popupContentSize.height - verticalMargin
        val y =
            sequenceOf(
                topToAnchorBottom,
                bottomToAnchorTop,
                centerToAnchorTop,
                bottomToWindowBottom
            )
                .firstOrNull {
                    it >= verticalMargin &&
                            it + popupContentSize.height <= windowSize.height - verticalMargin
                } ?: bottomToAnchorTop

        onPositionCalculated(
            anchorBounds,
            IntRect(x, y, x + popupContentSize.width, y + popupContentSize.height)
        )
        return IntOffset(x, y)
    }
}

/** @see DropDownMenu */
@Composable
fun BasicPopupMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    elevation: Dp = Dp.Unspecified,
    background: Background = Background(AppTheme.colors.background(1.dp)),
    contentColor: Color = AppTheme.colors.onBackground,
    shape: Shape = AppTheme.shapes.large,
    border: BorderStroke? = if (AppTheme.colors.isLight) null else BorderStroke(0.5.dp, AppTheme.colors.background(20.dp)),
    properties: PopupProperties = DefaultMenuProperties,
    content: @Composable () -> Unit
) {
    val expandedState = remember { MutableTransitionState(false) }
    expandedState.targetState = expanded

    if (expandedState.currentState || expandedState.targetState) {
        val transformOriginState = remember { mutableStateOf(TransformOrigin.Center) }
        val density = LocalDensity.current
        val popupPositionProvider =
            DropdownMenuPositionProvider(offset, density) { parentBounds, menuBounds ->
                transformOriginState.value = calculateTransformOrigin(parentBounds, menuBounds)
            }

        val content = @Composable {
            // Menu open/close animation.
            val transition = rememberTransition(expandedState, "DropDownMenu")

            //
            val scaleAnimationSpec = AppTheme.motionScheme.fastSpatialSpec<Float>()
            val alphaAnimationSpec = AppTheme.motionScheme.fastEffectsSpec<Float>()


            val scale by
            transition.animateFloat(transitionSpec = { scaleAnimationSpec }) { expanded ->
                if (expanded) ExpandedScaleTarget else ClosedScaleTarget
            }

            val alpha by
            transition.animateFloat(transitionSpec = { alphaAnimationSpec }) { expanded ->
                if (expanded) ExpandedAlphaTarget else ClosedAlphaTarget
            }

            // Surface
            // FixMe - It seems Popup is not using the app density hence providing here the
            //  app density instead of the popup density provided internally
            CompositionLocalProvider(LocalDensity provides density) {
                val isInspecting = LocalInspectionMode.current
                Surface(
                    modifier = Modifier.graphicsLayer {
                        scaleX =
                            if (!isInspecting) scale
                            else if (expandedState.targetState) ExpandedScaleTarget else ClosedScaleTarget
                        scaleY =
                            if (!isInspecting) scale
                            else if (expandedState.targetState) ExpandedScaleTarget else ClosedScaleTarget
                        this.alpha =
                            if (!isInspecting) alpha
                            else if (expandedState.targetState) ExpandedAlphaTarget else ClosedAlphaTarget
                        transformOrigin = transformOriginState.value
                    } then modifier,
                    elevation = elevation.takeOrElse { MenuElevation },
                    contentColor = contentColor,
                    content = content,
                    background = background,
                    border = border,
                    shape = shape,
                )
            }
        }
        Popup(
            onDismissRequest = onDismissRequest,
            popupPositionProvider = popupPositionProvider,
            properties = properties,
            content = content
        )
    }
}

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * ![Dropdown menu image](https://developer.android.com/images/reference/androidx/compose/material3/menu.png)
 *
 * A [DropdownMenu] behaves similarly to a [Popup], and will use the position of the parent layout
 * to position itself on screen. Commonly a [DropdownMenu] will be placed in a [Box] with a sibling
 * that will be used as the 'anchor'. Note that a [DropdownMenu] by itself will not take up any
 * space in a layout, as the menu is displayed in a separate window, on top of other content.
 *
 * The [content] of a [DropdownMenu] will typically be [DropdownMenuItem]s, as well as custom
 * content. Using [DropdownMenuItem]s will result in a menu that matches the Material specification
 * for menus. Also note that the [content] is placed inside a scrollable [Column], so using a
 * [LazyColumn] as the root layout inside [content] is unsupported.
 *
 * [onDismissRequest] will be called when the menu should close - for example when there is a tap
 * outside the menu, or when the back key is pressed.
 *
 * [DropdownMenu] changes its positioning depending on the available space, always trying to be
 * fully visible. Depending on layout direction, first it will try to align its start to the start
 * of its parent, then its end to the end of its parent, and then to the edge of the window.
 * Vertically, it will try to align its top to the bottom of its parent, then its bottom to top of
 * its parent, and then to the edge of the window.
 *
 * An [offset] can be provided to adjust the positioning of the menu for cases when the layout
 * bounds of its parent do not coincide with its visual bounds.
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.MenuSample
 *
 * Example usage with a [ScrollState] to control the menu items scroll position:
 *
 * @sample androidx.compose.material3.samples.MenuWithScrollStateSample
 * @param expanded whether the menu is expanded or not
 * @param onDismissRequest called when the user requests to dismiss the menu, such as by tapping
 *   outside the menu's bounds
 * @param modifier [Modifier] to be applied to the menu's content
 * @param offset [DpOffset] from the original position of the menu. The offset respects the
 *   [LayoutDirection], so the offset's x position will be added in LTR and subtracted in RTL.
 * @param scrollState a [ScrollState] to used by the menu's content for items vertical scrolling
 * @param properties [PopupProperties] for further customization of this popup's behavior
 * @param shape the shape of the menu
 * @param containerColor the container color of the menu
 * @param contentColor the content color within the items of the menu.
 * @param elevation the elevation for the shadow below the menu
 * @param border the border to draw around the container of the menu. Pass `null` for no border.
 * @param content the content of this dropdown menu, typically a [DropdownMenuItem]
 */
@Composable
@NonRestartableComposable
fun DropDownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    elevation: Dp = Dp.Unspecified,
    background: Background = Background(AppTheme.colors.background(1.dp)),
    contentColor: Color = AppTheme.colors.onBackground,
    shape: Shape = AppTheme.shapes.large,
    border: BorderStroke? = if (AppTheme.colors.isLight) null else BorderStroke(0.5.dp, AppTheme.colors.background(20.dp)),
    scrollState: ScrollState? = null,
    properties: PopupProperties = DefaultMenuProperties,
    content: @Composable ColumnScope.() -> Unit
) = BasicPopupMenu(
    expanded = expanded,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
    offset = offset,
    elevation = elevation,
    background = background,
    contentColor = contentColor,
    shape = shape,
    border = border,
    properties = properties
) {
    Column(
        modifier = modifier
            .padding(vertical = DropdownMenuVerticalPadding)
            .width(IntrinsicSize.Max)
            .thenIf(scrollState != null) { verticalScroll(scrollState!!) },
        content = content
    )
}

/**
 * [Material Design dropdown menu](https://m3.material.io/components/menus/overview)
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * [Dropdown menu image](https://developer.android.com/images/reference/androidx/compose/material3/menu.png)
 *
 * Example usage:
 *
 * @sample androidx.compose.material3.samples.MenuSample
 * @param text text of the menu item
 * @param onClick called when this menu item is clicked
 * @param modifier the [Modifier] to be applied to this menu item
 * @param leadingIcon optional leading icon to be displayed at the beginning of the item's text
 * @param trailingIcon optional trailing icon to be displayed at the end of the item's text. This
 *   trailing icon slot can also accept [Text] to indicate a keyboard shortcut.
 * @param enabled controls the enabled state of this menu item. When `false`, this component will
 *   not respond to user input, and it will appear visually disabled and disabled to accessibility
 *   services.
 * @param colors [MenuItemColors] that will be used to resolve the colors used for this menu item in
 *   different states. See [MenuDefaults.itemColors].
 * @param contentPadding the padding applied to the content of this menu item
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting [Interaction]s for this menu item. You can use this to change the menu item's
 *   appearance or preview the menu item in different states. Note that if `null` is provided,
 *   interactions will still happen internally.
 */
@Composable
fun DropdownMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit
){
    // TODO: investigate replacing this Row with ListItem
    Row(
        modifier =
        modifier
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(true)
            )
            .fillMaxWidth()
            // Preferred min and max width used during the intrinsic measurement.
            .sizeIn(
                minWidth = DropdownMenuItemDefaultMinWidth,
                maxWidth = DropdownMenuItemDefaultMaxWidth,
                minHeight = DropdownMenuItemDefaultMinHeight
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val typography = AppTheme.typography
        ProvideTextStyle (typography.label1) {
            val contentAlpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled
            CompositionLocalProvider(LocalContentColor provides LocalContentColor.current.copy(contentAlpha)) { content() }
        }
    }
}

// Placeholder icon size, for items with no icon.
private val PlaceholderIconSizeModifier = Modifier.size(24.dp)

/**
 * @see DropDownMenuItem
 */
@Composable
@NonRestartableComposable
fun DropDownMenuItem(
    title: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: CharSequence? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    DropdownMenuItem(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        interactionSource = interactionSource
    ) {
        if (icon != null)
            com.zs.compose.theme.Icon(
                imageVector = icon,
                contentDescription = title.toString(),
                //  modifier = Modifier.padding(start = 16.dp)
            )
        else
            Spacer(PlaceholderIconSizeModifier)

        // the text
        Text(
            text = buildAnnotatedString {
                append(title)
                if (subtitle == null) return@buildAnnotatedString
                withStyle(
                    SpanStyle(
                        color = LocalContentColor.current.copy(ContentAlpha.disabled),
                        fontSize = 11.sp,
                    ),
                    block = {
                        append("\n" + subtitle)
                    }
                )
            },
            modifier = Modifier.padding(horizontal = 16.dp),
            maxLines = if (subtitle == null) 1 else 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}