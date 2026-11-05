package com.zs.compose.foundation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxOfOrNull

private const val TAG = "Slot"

/**
 * A minimal Box-like container that expects a single child and centers it.
 * Extra children are stacked at the center too, and a warning is logged.
 */
@Composable
inline fun Slot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = SlotMeasurePolicy
    )
}

@PublishedApi
internal object SlotMeasurePolicy : MeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return when (measurables.size) {
            // No content: take the minimum size, same as Box.
            0 -> layout(constraints.minWidth, constraints.minHeight) {}

            // Fast path: no list allocation, no iteration.
            1 -> {
                val placeable =
                    measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
                val width = maxOf(constraints.minWidth, placeable.width)
                val height = maxOf(constraints.minHeight, placeable.height)
                layout(width, height) {
                    placeCentered(placeable, width, height, layoutDirection)
                }
            }

            // Fallback: stack everything at the center.
            else -> {
                Log.w(
                    TAG,
                    "Slot expects a single child but got ${measurables.size}. " +
                            "All children will be stacked at the center."
                )
                val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = measurables.fastMap { it.measure(childConstraints) }
                val width =
                    maxOf(constraints.minWidth, placeables.fastMaxOfOrNull { it.width } ?: 0)
                val height =
                    maxOf(constraints.minHeight, placeables.fastMaxOfOrNull { it.height } ?: 0)
                layout(width, height) {
                    placeables.fastForEach {
                        placeCentered(it, width, height, layoutDirection)
                    }
                }
            }
        }
    }
}

private fun Placeable.PlacementScope.placeCentered(
    placeable: Placeable,
    width: Int,
    height: Int,
    layoutDirection: LayoutDirection
) {
    val position = Alignment.Center.align(
        size = IntSize(placeable.width, placeable.height),
        space = IntSize(width, height),
        layoutDirection = layoutDirection
    )
    placeable.place(position)
}