package com.zs.compose.theme.common

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/**
 * Lays out [icon] and [label] along [orientation], separated by [spacing].
 *
 * Children are placed in order (icon first, then label) and the pair is centered
 * within the available space.
 */
@Composable
@PublishedApi
internal inline fun IconLabelLayout(
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Horizontal,
    spacing: Dp = 8.dp,
    crossinline icon: @Composable () -> Unit,
    crossinline label: @Composable () -> Unit,
) {
    val policy = remember(orientation, spacing) { IconLabelMeasurePolicy(orientation, spacing) }
    Layout(
        content = {
            icon()
            label()
        },
        modifier = modifier,
        measurePolicy = policy,
    )
}

@PublishedApi
internal class IconLabelMeasurePolicy(
    private val orientation: Orientation,
    private val spacing: Dp,
) : MeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val horizontal = orientation == Orientation.Horizontal
        val gap = spacing.roundToPx()

        // Children may be smaller than the container; centering happens at placement.
        val loose = constraints.copy(minWidth = 0, minHeight = 0)

        // The icon gets everything; the label gets what is left after the icon and the gap.
        val icon = measurables[0].measure(loose)
        val used = (if (horizontal) icon.width else icon.height) + gap
        val label = measurables[1].measure(
            if (horizontal) loose.offset(horizontal = -used) else loose.offset(vertical = -used)
        )

        val iconMain = if (horizontal) icon.width else icon.height
        val labelMain = if (horizontal) label.width else label.height
        val iconCross = if (horizontal) icon.height else icon.width
        val labelCross = if (horizontal) label.height else label.width

        val contentMain = iconMain + gap + labelMain
        val contentCross = maxOf(iconCross, labelCross)

        val width = (if (horizontal) contentMain else contentCross)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (if (horizontal) contentCross else contentMain)
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        return layout(width, height) {
            val containerMain = if (horizontal) width else height
            val containerCross = if (horizontal) height else width
            val mainStart = (containerMain - contentMain) / 2

            val iconCrossPos = (containerCross - iconCross) / 2
            val labelCrossPos = (containerCross - labelCross) / 2
            val labelMainPos = mainStart + iconMain + gap

            if (horizontal) {
                icon.place(mainStart, iconCrossPos)
                label.place(labelMainPos, labelCrossPos)
            } else {
                icon.place(iconCrossPos, mainStart)
                label.place(labelCrossPos, labelMainPos)
            }
        }
    }
}