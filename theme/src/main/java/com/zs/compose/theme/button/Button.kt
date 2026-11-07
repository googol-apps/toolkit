@file:OptIn(ExperimentalFoundationStyleApi::class)

package com.zs.compose.theme.button

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.style.CustomStyle
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.StyleScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zs.compose.foundation.style.Style

interface ButtonStyleScope : StyleScope
fun interface ButtonStyle : CustomStyle<ButtonStyleScope> {
    companion object : ButtonStyle {
        override fun ButtonStyleScope.applyStyle() {}
    }
}

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
inline fun BasicButton(
    noinline onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Horizontal,
    interactions: MutableInteractionSource? = null,
    style: ButtonStyle = ButtonStyle
) {


}