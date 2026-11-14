@file:OptIn(ExperimentalFoundationStyleApi::class)

package com.zs.compose.theme.button

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.disabled
import androidx.compose.foundation.style.focused
import androidx.compose.foundation.style.hovered
import androidx.compose.foundation.style.pressed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.Slot
import com.zs.compose.foundation.style.Style
import com.zs.compose.foundation.style.properties.background
import com.zs.compose.foundation.style.properties.border
import com.zs.compose.foundation.style.properties.elevation
import com.zs.compose.foundation.style.properties.minSize
import com.zs.compose.foundation.style.properties.padding
import com.zs.compose.foundation.style.properties.scale
import com.zs.compose.foundation.style.properties.shape
import com.zs.compose.theme.Shapes
import com.zs.compose.theme.colors
import com.zs.compose.theme.shapes

object Button {
    private const val TAG = "Button"

    // ── Size & spacing (flip with orientation) ──────────────────────────────
   val HorizontalMinSize = DpSize(width = 56.dp, height = 40.dp)
   val VerticalMinSize = DpSize(width = 40.dp, height = 56.dp)

   val HorizontalContentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
   val VerticalContentPadding = PaddingValues(horizontal = 6.dp, vertical = 12.dp)

    // Gap between icon and label. Stacked layouts usually want a tighter gap.
    val HorizontalIconSpacing = 8.dp
    val VerticalIconSpacing = 4.dp

    // ── Icon (same in both orientations) ────────────────────────────────────
    private val IconSize = 18.dp

    // ── Elevation per state ─────────────────────────────────────────────────
    val DefaultElevation = 0.dp
    val HoveredElevation = 3.dp
    val FocusedElevation = 4.dp
    val PressedElevation = 6.dp
    val DisabledElevation = 0.dp

    // ── Disabled ────────────────────────────────────────────────────────────
    private const val DisabledContainerAlpha = 0.12f
    private const val DisabledContentAlpha = 0.38f

    private const val PressedScale = 1.1f         // optional press feedback
    private const val FocussedScale = 1.05f
    private const val HoveredScale = 1.05f


    val filledButtonStyle: Style = {
        minSize(VerticalMinSize)
        padding(HorizontalContentPadding)
        elevation(DefaultElevation)
        background(colors.accent)
        shape(shapes.xSmall)

        disabled {
            animate {
                background(colors.accent.copy(DisabledContainerAlpha).compositeOver(colors.background))
            }
        }


        pressed {
            animate(){
                background(colors.accent)
                scale(PressedScale)
                shape(shapes.medium)
                elevation(PressedElevation)
                border(2.dp, colors.onAccent)
            }
        }

        hovered {
            animate(){
                scale(HoveredScale)
                shape(shapes.small)
                elevation(HoveredElevation)
                border(1.dp, colors.onAccent)
            }
        }

        focused {
            animate(){
                scale(FocussedScale)
                shape(shapes.small)
                elevation(FocusedElevation)
                border(1.dp, colors.onAccent)
            }
        }
    }

    /**
     * Button with an optional [icon] and a [label].
     *
     * The [icon] is placed before the [label] along [orientation], separated by [spacing]. Both are
     * composed into a single content block for [BasicButton], which handles layout, styling and clicks.
     *
     * @param onClick Called when the button is clicked.
     * @param modifier Modifier applied to the button.
     * @param enabled Whether the button reacts to input. Also exposed to [style] as the enabled state.
     * @param orientation Axis along which the icon and label are arranged.
     * @param spacing Gap between the icon and the label.
     * @param style Style resolved against the button's interaction state (pressed, hovered, etc.).
     * @param interactions Optional source for observing or driving interactions. A private one is
     * created when null.
     * @param icon Optional icon shown before the label. Empty by default.
     * @param label Text or content describing the action. Trailing lambda.
     */
    @Composable
    inline operator fun invoke(
        noinline onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        orientation: Orientation = Orientation.Horizontal,
        spacing: Dp = 0.dp,
        style: Style = filledButtonStyle,
        interactions: MutableInteractionSource? = null,
        crossinline icon: @Composable () -> Unit = {},
        crossinline label: @Composable () -> Unit,
    ) = BasicButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        orientation = orientation,
        spacing = if (orientation == Orientation.Horizontal) HorizontalIconSpacing else VerticalIconSpacing,
        style = style,
        interactions = interactions,
        // Content contract expected by IconLabelMeasurePolicy: icon first, then label.
        content = {
            icon()
            label()
        },
    )
}