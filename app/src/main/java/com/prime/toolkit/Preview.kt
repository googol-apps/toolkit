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

@file:OptIn(ExperimentalThemeApi::class)

package com.prime.toolkit

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.zs.compose.foundation.decorator.decorator
import com.zs.compose.theme.ExperimentalThemeApi
import com.zs.compose.theme.adaptive.content

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
                    .decorator(
                        backgroundColor = Color.Unspecified,
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
            Preview()
        }
    }
}