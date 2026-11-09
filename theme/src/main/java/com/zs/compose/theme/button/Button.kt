@file:OptIn(ExperimentalFoundationStyleApi::class)

package com.zs.compose.theme.button

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.pressed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.MetroGreen
import com.zs.compose.foundation.style.Style
import com.zs.compose.foundation.style.properties.background
import com.zs.compose.foundation.style.properties.border
import com.zs.compose.foundation.style.properties.elevation
import com.zs.compose.foundation.style.properties.margin
import com.zs.compose.foundation.style.properties.minSize
import com.zs.compose.foundation.style.properties.shape
import com.zs.compose.foundation.style.styleable
import com.zs.compose.theme.colors
import com.zs.compose.theme.shapes

object Button {

    private const val TAG = "Button"

    val filledButtonStyle: Style = {
        minSize(58.dp, 36.dp)
        background(colors.accent)
        elevation(5.dp)
        shape(shapes.xSmall)
        margin(12.dp, 6.dp)
        margin(0.dp, 2.dp, 0.dp, 0.dp)
        pressed {
            shape(shapes.medium)
            elevation(10.dp)
        }
    }

    @Composable
    operator fun invoke(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        style: Style = {
            minSize(58.dp, 40.dp)
            background(colors.accent)
            elevation(10.dp)
            shape(RoundedCornerShape(5))
            margin(12.dp, 6.dp)
            margin(2.dp)
            border(4.dp, colors.onAccent)
            Log.d(TAG, "style: deafult")
            pressed {
                this.animate {
                    background(Color.MetroGreen)
                    Log.d(TAG, "style: pressed")
                    shape(RoundedCornerShape(50))
                    elevation(120.dp)
                    border(4.dp, colors.onAccent)
                }
            }
        },
        enabled: Boolean = true,
        orientation: Orientation = Orientation.Horizontal,
        interactions: MutableInteractionSource? = null,
        icon: @Composable () -> Unit,
        label: @Composable () -> Unit
    ) {
        val finalInteractions = interactions ?: remember(::MutableInteractionSource)
        //val state = remember(finalInteractions) { MutableStyleState(finalInteractions) }
        //state.isEnabled = enabled
        Row(
            modifier = modifier
                .styleable(finalInteractions, style)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    role = Role.Button,
                    interactionSource = finalInteractions,
                    indication = null
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
            ) {
            icon()
            label()
        }
    }
}