package com.zs.compose.theme.button

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.zs.compose.foundation.style.Style
import com.zs.compose.foundation.style.styleable

// Layouts, container shown as the outer box (c = center line of the container):
//
//  Horizontal, two children         Horizontal, one child
//  ┌──────────────────────────┐     ┌──────────────────────────┐
//  │                          │     │                          │
//  │      ┌────┐ gap ┌─────┐  │     │          ┌─────┐         │
//  │ c ───│icon│─────│label│──│     │ c ───────│label│─────────│
//  │      └────┘     └─────┘  │     │          └─────┘         │
//  │      |<-- block -->|     │     │                          │
//  └──────────────────────────┘     └──────────────────────────┘
//   block centered on x, each child centered on y (the center line c)
//
//  Vertical, two children           Vertical, one child
//  ┌────────────────┐               ┌────────────────┐
//  │     ┌────┐     │ ─┐            │                │
//  │     │icon│     │  │            │                │
//  │     └────┘     │  │ block      │    ┌──────┐    │
//  │      gap       │  │            │ c ─│ icon │─   │
//  │    ┌───────┐   │  │            │    └──────┘    │
//  │ c ─│ label │─  │ ─┘            │                │
//  │    └───────┘   │               │                │
//  └────────────────┘               └────────────────┘
//   block centered on y, each child centered on x (the center line c)
//
//  No children: nothing is placed, container = minimum constraints.
//
// Measure flow, main axis shown (horizontal; vertical works the same with width/height swapped):
//
//  parent max ............ |<------------------------ max ------------------------>|
//  icon (first child) .... |<------------------------ max ------------------------>|
//                          used = 0, so no leading gap
//  label (second child) .. |  icon  | gap |<---------- max - icon - gap ---------->|
//                          used = icon + gap, so a long label can't push past the icon
//  contentMain = icon + gap + label  (gap only exists between two children)
//  contentCross = max(icon, label)   (size across the axis)
//
// Placement (main axis):
//
//  |<------------------------ containerMain ------------------------>|
//  |   free / 2   |<-------- contentMain -------->|   free / 2       |
//                 ^ main starts here
//                 icon at main, label at main + icon + gap
//
//  free = containerMain - contentMain. If it is negative (content bigger than the container),
//  the block overflows equally on both sides instead of being cut at one end.

/**
 * Arranges an optional icon and an optional label along [orientation], centered in the container.
 *
 * Empty slots emit no nodes, so the children are treated as an ordered list (icon first, then
 * label). [spacing] is applied only between two children, never before, after or around a single
 * one. With no children, the layout falls back to the minimum constraints. Placement uses
 * `placeRelative`, so a horizontal row mirrors in RTL.
 *
 * Terms used in the comments:
 * - main axis:  the stacking direction (x for Horizontal, y for Vertical)
 * - cross axis: the other direction; every child is centered on it
 */
internal class ButtonMeasurePolicy(
    private val orientation: Orientation,
    private val spacing: Dp,
) : MeasurePolicy {

    private val horizontal get() = orientation == Orientation.Horizontal

    // Layouts (c = center line of the container):
    //
    //  Horizontal, two children         Vertical, two children
    //  ┌──────────────────────────┐     ┌────────────────┐
    //  │      ┌────┐ gap ┌─────┐  │     │     ┌────┐     │ ─┐
    //  │ c ───│icon│─────│label│──│     │     │icon│     │  │ block, centered
    //  │      └────┘     └─────┘  │     │     └────┘     │  │ on y
    //  │      |<-- block -->|     │     │      gap       │  │
    //  └──────────────────────────┘     │    ┌───────┐   │  │
    //   block centered on x             │ c ─│ label │─  │ ─┘
    //   each child centered on y        │    └───────┘   │
    //                                   └────────────────┘
    //  One child: centered on both axes.  No children: minimum constraints.
    //
    // Measure flow, main axis (each child gets what is left after earlier children and gaps):
    //
    //  icon  ... |<---------------------- max ---------------------->|   used = 0, no leading gap
    //  label ... | icon | gap |<--------- max - icon - gap --------->|
    //
    // Placement, main axis:
    //
    //  |<------------------- containerMain ------------------->|
    //  |  free / 2  |<------ contentMain ------>|  free / 2    |
    //               ^ main starts here: icon at main, label at main + icon + gap
    //  If free is negative the block overflows equally on both sides.

    // ── Intrinsics ──────────────────────────────────────────────────────────
    // The default implementation runs measure() with fake placeables whose size is the whole
    // available space. The first child would then use up everything, the label would get ~0
    // width, wrap one character per line, and report a huge intrinsic height. Parents that read
    // intrinsics (such as the style node sizing the box) would then make the button far too big.
    //
    //  main axis:  sum of the children's intrinsics, with the gap only between children
    //  cross axis: max over the children, each given the main space left after the earlier
    //              children, the same rule as measure()

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>, height: Int,
    ) = if (horizontal) mainIntrinsic(measurables, height, max = false)
    else crossIntrinsic(measurables, height, max = false)

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>, height: Int,
    ) = if (horizontal) mainIntrinsic(measurables, height, max = true)
    else crossIntrinsic(measurables, height, max = true)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>, width: Int,
    ) = if (horizontal) crossIntrinsic(measurables, width, max = false)
    else mainIntrinsic(measurables, width, max = false)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>, width: Int,
    ) = if (horizontal) crossIntrinsic(measurables, width, max = true)
    else mainIntrinsic(measurables, width, max = true)

    /** Size along the stacking axis: children side by side, gap only between them. */
    private fun IntrinsicMeasureScope.mainIntrinsic(
        measurables: List<IntrinsicMeasurable>,
        crossSize: Int,
        max: Boolean,
    ): Int {
        val gap = spacing.roundToPx()
        var total = 0
        measurables.forEachIndexed { index, child ->
            if (index > 0) total += gap
            total += child.mainIntrinsic(max, crossSize)
        }
        return total
    }

    /** Size across the stacking axis: the largest child at the main space it would get. */
    private fun IntrinsicMeasureScope.crossIntrinsic(
        measurables: List<IntrinsicMeasurable>,
        mainSize: Int,
        max: Boolean,
    ): Int {
        val gap = spacing.roundToPx()
        var used = 0
        var cross = 0
        measurables.forEachIndexed { index, child ->
            val gapBefore = if (index > 0) gap else 0
            // Main space left for this child after earlier children and the gap.
            val available = (mainSize - used - gapBefore).coerceAtLeast(0)
            cross = maxOf(cross, child.crossIntrinsic(max, available))
            // Advance by the child's real main size, never by the whole available space.
            used += gapBefore + minOf(available, child.mainIntrinsic(max = true, Constraints.Infinity))
        }
        return cross
    }

    private fun IntrinsicMeasurable.mainIntrinsic(max: Boolean, crossSize: Int) = when {
        horizontal && max -> maxIntrinsicWidth(crossSize)
        horizontal -> minIntrinsicWidth(crossSize)
        max -> maxIntrinsicHeight(crossSize)
        else -> minIntrinsicHeight(crossSize)
    }

    private fun IntrinsicMeasurable.crossIntrinsic(max: Boolean, mainSize: Int) = when {
        horizontal && max -> maxIntrinsicHeight(mainSize)
        horizontal -> minIntrinsicHeight(mainSize)
        max -> maxIntrinsicWidth(mainSize)
        else -> minIntrinsicWidth(mainSize)
    }

    // ── Measure and place ───────────────────────────────────────────────────
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        val gap = spacing.roundToPx()

        // Size along the stacking axis (main) and across it (cross).
        fun Placeable.main() = if (horizontal) width else height
        fun Placeable.cross() = if (horizontal) height else width

        // Children may be smaller than the container; centering happens at placement.
        val loose = constraints.copy(minWidth = 0, minHeight = 0)

        // 1. Measure every child first: placement needs the total block size to center it.
        val placeables = ArrayList<Placeable>(measurables.size)
        var contentMain = 0
        var contentCross = 0
        for (measurable in measurables) {
            // Space taken so far, including the gap. Zero for the first child, so no leading gap.
            val used = if (placeables.isEmpty()) 0 else contentMain + gap
            val placeable = measurable.measure(
                if (horizontal) loose.offset(horizontal = -used) else loose.offset(vertical = -used)
            )
            contentMain = used + placeable.main()
            contentCross = maxOf(contentCross, placeable.cross())
            placeables += placeable
        }

        // 2. Container size; with no children this falls back to the minimum constraints.
        val width = (if (horizontal) contentMain else contentCross)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (if (horizontal) contentCross else contentMain)
            .coerceIn(constraints.minHeight, constraints.maxHeight)

        // 3. Place: center the whole block on the main axis, each child on the cross axis.
        return layout(width, height) {
            val containerMain = if (horizontal) width else height
            val containerCross = if (horizontal) height else width

            // Not clamped, so overflow is symmetric.
            var main = (containerMain - contentMain) / 2
            for (placeable in placeables) {
                val cross = (containerCross - placeable.cross()) / 2
                // placeRelative mirrors x in RTL; vertical is unaffected because cross is centered.
                if (horizontal) placeable.placeRelative(main, cross)
                else placeable.placeRelative(cross, main)
                main += placeable.main() + gap
            }
        }
    }
}

/**
 * Base button layout shared by all button variants.
 *
 * Lays out [content] (typically an icon and a label) along [orientation] with [spacing] between
 * items, applies [style] for state-based visuals, and handles clicks with button semantics.
 *
 * This is an internal building block. Public buttons should not expose [content] to users.
 * They take explicit `icon` and `label` parameters and compose them into [content] here, so
 * callers can't break the layout contract the measure policy expects.
 *
 * It is deliberately a regular (non-inline) composable. That gives the button its own
 * recomposition scope, so a change to a button's inputs (such as [enabled]) recomposes only
 * this button instead of the whole parent, and the call can be skipped when its parameters are
 * unchanged. An `inline` function would merge into the caller's scope and lose that isolation.
 *
 * @param onClick Called when the button is clicked.
 * @param modifier Modifier applied to the button.
 * @param enabled Whether the button reacts to input. Also exposed to [style] as the enabled state.
 * @param orientation Axis along which the content items are arranged.
 * @param spacing Gap between content items along [orientation].
 * @param style Style resolved against the button's interaction state (pressed, hovered, etc.).
 * @param interactions Optional source for observing or driving interactions. A private one is
 * created when null.
 * @param content Button content, usually an icon and/or a label. Internal only; public APIs
 * pass `icon` and `label` instead.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
@PublishedApi
internal fun BasicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Horizontal,
    spacing: Dp = 0.dp,
    style: Style = Style,
    interactions: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    // Measure policy: arranges the content items along the orientation with the given gap.
    // Recreated only when either input changes.
    val policy = remember(orientation, spacing) { ButtonMeasurePolicy(orientation, spacing) }

    // Interaction and style state: use the caller's source if given, otherwise own one.
    // The style state listens to the source and mirrors `enabled`, so the style can react to both.
    val finalInteractions = interactions ?: remember(::MutableInteractionSource)
    val state = remember(finalInteractions) { MutableStyleState(finalInteractions) }.also {
        it.isEnabled = enabled
    }

    Layout(
        content = content,
        measurePolicy = policy,
        modifier = modifier
            // Visuals: the style reads `state` for pressed/hovered/enabled changes.
            .styleable(state, style)
            // Behavior: click handling with button semantics. `indication = null` because the
            // style draws the state feedback itself; the same source keeps both in sync.
            .clickable(
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
                interactionSource = finalInteractions,
                indication = null,
            ),
    )
}