/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 03-05-2025.
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

@file:OptIn(ExperimentalThemeApi::class, ExperimentalFoundationStyleApi::class)

package com.prime.toolkit

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.foundation.style.StyleScope
import androidx.compose.foundation.style.StyleState
import androidx.compose.foundation.style.StyleStateKey
import androidx.compose.foundation.style.animate
import androidx.compose.foundation.style.pressed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.zs.compose.foundation.JetBlack
import com.zs.compose.foundation.MetroGreen
import com.zs.compose.foundation.OrientRed
import com.zs.compose.foundation.Slot
import com.zs.compose.foundation.style.styleable
import com.zs.compose.foundation.style.background
import com.zs.compose.foundation.style.border
import com.zs.compose.foundation.style.elevation
import com.zs.compose.foundation.style.minSize
import com.zs.compose.foundation.style.noise
import com.zs.compose.foundation.style.padding
import com.zs.compose.foundation.style.scale
import com.zs.compose.foundation.style.shape
import com.zs.compose.foundation.style.size
import com.zs.compose.foundation.style.translate
import com.zs.compose.theme.ExperimentalThemeApi
import com.zs.compose.theme.LocalRippleConfiguration
import com.zs.compose.theme.RippleConfiguration
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.text.Label

private const val TAG = "Preview"

@Composable
private fun LoroumImage(uri: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data(uri)
            .build(),
        onError = { Log.d(TAG, "Game: ${it.result.throwable.message}") },
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
fun PreviewDecorator(modifier: Modifier = Modifier) {
    Column(modifier) {
        //
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            LoroumImage(
                "https://cdn.thegamesdb.net/images/original/boxart/front/53-1.jpg",
                modifier = Modifier
                    .size(256.dp)
                    .styleable(
                        backgroundColor = Unspecified,
                        shape = RoundedCornerShape(100),
                        border = BorderStroke(2.dp, Color.Red),
                        elevation = 10.dp,
                        // foregroundColor = Color.Black.copy(0.5f),
                        outline = BorderStroke(2.dp, Color.Blue),
                        outlineGap = 10.dp,
                        outlinePathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(36f, 12f),
                            phase = 0f
                        ),
                        noiseAlpha = 1f,
                    )
            )
        }
    }
}

// 1. Create a custom StyleStateKey with a default value
val SelectedStatekey = StyleStateKey(defaultValue = false)


// 2. Add an extension property to easily get/set the state on MutableStyleState
var StyleState.selected: Boolean
    get() = this[SelectedStatekey]
    set(value) {
        if (this is MutableStyleState) {
            this[SelectedStatekey] = value
        }
    }

// 3. Create a conditional scope block for the Style DSL
inline fun StyleScope.selected(crossinline block: StyleScope.() -> Unit) {
    if (state.selected) {
        block()
    }
}

@Composable
fun PreviewStyllable(modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val styleState = remember { MutableStyleState(interactionSource) }
    styleState.isChecked = selected

    Log.d(TAG, "PreviewStyllable: ")
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            color = Red
        )
    ) {
        Slot(
            modifier = Modifier
                .styleable(
                    interactions = interactionSource,
                ) {
                    Log.d(TAG, "PreviewSyllable: default")
                    background(Color.OrientRed)
                    size(100.dp, 48.dp)
                    noise(0.4f)
                    //   padding(PaddingValues(20.dp))
                    shape(RoundedCornerShape(10))
                    border(BorderStroke(1.dp, Color.White))
                    elevation(5.dp)
                    animate(tween(500)) {
                        pressed {
                            padding(5.dp)
                            //scale(1.25f)
                            noise(1f)
                            border(BorderStroke(2.dp, Color.White))
                            elevation(20.dp)
                        }
                    }
                }
                .clickable(interactionSource = interactionSource) {
                    selected = !selected
                }
        ) {
            Label(text = "Button2")
        }

        Slot(
            modifier = Modifier
                .styleable(
                    interactions = interactionSource,
                ) {
                    Log.d(TAG, "PreviewStyllable: defult")
                    background(Color.OrientRed)
                    size(100.dp, 48.dp)

                    noise(0.4f)
                    //   padding(PaddingValues(20.dp))
                    shape(RoundedCornerShape(10))
                    border(BorderStroke(1.dp, Color.White))
                    elevation(5.dp)
                    animate(tween(500)) {
                        pressed {
                            padding(5.dp)
                            //scale(1.25f)
                            noise(1f)
                            border(BorderStroke(2.dp, Color.White))
                            elevation(20.dp)
                        }
                    }
                }
                .clickable(interactionSource = interactionSource) {
                    selected = !selected
                }


        ) {
            Label(text = "Button2")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun Preview() {
    PreviewDecorator(modifier = Modifier.fillMaxWidth())
}

@Composable
fun PreviewNode() {
    com.zs.compose.theme.adaptive.Scaffold(topBar = {
        com.zs.compose.theme.appbar.TopAppBar(title = { com.zs.compose.theme.text.Label("Preview") })
    }) {
        val insets = WindowInsets.content
        Column(/*modifier = Modifier.padding(16.dp).windowInsetsPadding(insets)*/) {
            PreviewStyllable()
        }
    }
}