package com.zs.compose.foundation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.max
/**
 * Measure policy for `Slot`, a minimal [Box]-like container that centers its content.
 *
 * ## Intended use
 * Slot expects exactly **one** child. Extra children are still measured, stacked on top of
 * each other and centered, but a warning is logged because that is not the intended use.
 *
 * ## Constraints
 * Two sets of constraints are involved, and they must not be mixed up:
 *
 * - **Child constraints:** the incoming constraints with the minimum relaxed to 0
 *   (see [Constraints.copyMaxDimensions]). The child may be smaller than the Slot and is
 *   free to pick its natural size, up to the incoming maximum. This is the same as
 *   `Box(propagateMinConstraints = false)`.
 * - **Slot constraints:** the original, unmodified incoming constraints. They decide the
 *   Slot's own size, so a parent (or a style node) that passes a tight size such as
 *   `200.dp..200.dp` gets exactly that size, with the child centered inside it.
 *
 * ## Sizing
 * - **No children:** the Slot takes the smallest size the parent allows.
 * - **With children:** the Slot wraps the largest child, then is coerced into the incoming
 *   constraints. It is therefore never smaller than the incoming min and never larger
 *   than the incoming max.
 *
 * ## Placement
 * Every child is centered in the Slot with [Alignment.Center], which respects the layout
 * direction.
 */
val SlotMeasurePolicy: MeasurePolicy = MeasurePolicy { measurables, constraints ->

    // No content: nothing to wrap, so take the smallest size the parent allows.
    if (measurables.isEmpty()) {
        return@MeasurePolicy layout(constraints.minWidth, constraints.minHeight) {}
    }

    if (measurables.size > 1) {
        Log.w("Slot", "The recommended max children for Slot is 1")
    }

    // Children are measured with the min relaxed to 0, so they can be smaller than the Slot.
    // This is for children only. The Slot itself is sized from `constraints` below.
    val childConstraints = constraints.copyMaxDimensions()

    var contentWidth = 0
    var contentHeight = 0
    val placeables = Array(measurables.size) { index ->
        measurables[index].measure(childConstraints).also {
            contentWidth = max(contentWidth, it.width)
            contentHeight = max(contentHeight, it.height)
        }
    }

    // Slot size: wrap the largest child, but stay within the incoming min/max.
    // This is the step that honors a tight parent size.
    val width = constraints.constrainWidth(contentWidth)
    val height = constraints.constrainHeight(contentHeight)

    layout(width, height) {
        val slotSize = IntSize(width, height)
        placeables.forEach { placeable ->
            val position = Alignment.Center.align(
                size = IntSize(placeable.width, placeable.height),
                space = slotSize,
                layoutDirection = layoutDirection,
            )
            placeable.place(position)
        }
    }
}

/**
 * A simple layout container that centers its child within the available space.
 *
 * ### Usage
 * - **Recommended children:** 0 or 1.
 *   - If no child is provided, the layout resolves to its minimum constraints.
 *   - If a single child is provided, it is measured and placed at the center.
 *   - If multiple children are provided, all are measured and centered, but this
 *     usage is discouraged and will log a warning.
 *
 * ### Behavior
 * - Respects minimum width/height constraints.
 * - Expands to fit the largest child when multiple children are present.
 * - Aligns children using [Alignment.Center].
 *
 * ### Notes
 * - This composable is intended as a lightweight, predictable container.
 * - Prefer using it for simple centering scenarios rather than complex layouts.
 *
 * @param modifier Optional [Modifier] for styling or layout adjustments.
 * @param content The child composable(s) to be placed inside the container.
 */
@Composable
inline fun Slot(content: @Composable () -> Unit) =
    Layout(content, Modifier, SlotMeasurePolicy)

/**
 * @see Slot
 */
@Composable
inline fun Slot(modifier: Modifier, content: @Composable () -> Unit) =
    Layout(content, modifier, SlotMeasurePolicy)