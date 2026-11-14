package com.zs.compose.theme.button

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.style.Style
import com.zs.compose.foundation.style.styleable

/**
 * Measures button content: 0, 1 or 2 children, by convention `[icon, label]`.
 *
 * ```
 * 0 -> empty, sized to min constraints
 * 1 -> [ x ] centred
 * 2 -> Horizontal: [icon] <sp> [label]    (mirrored in RTL)
 *      Vertical:   [icon] / <sp> / [label] (RTL-agnostic)
 * ```
 *
 * @param orientation Main axis, used only when there are two children.
 * @param spacing Gap between icon and label, used only when there are two children.
 */
internal class ButtonMeasurePolicy(
    private val orientation: Orientation,
    private val spacing: Dp,
) : MeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {

        // ── Validation ──
        // Fail fast: the policy only knows how to lay out [icon, label].
        require(measurables.size <= 2) { "Button supports at most 2 children (icon, label)." }

        // ── Case 0: nothing to place ──
        // Size falls back to the min constraints, so modifiers like widthIn(min = ..) or
        // defaultMinSize still give an empty button its minimum footprint.
        if (measurables.isEmpty()) {
            return layout(constraints.minWidth, constraints.minHeight) {}
        }

        // ── Helpers ──
        // Subtracts [used] from a max constraint, leaving Constraints.Infinity untouched
        // (an unbounded axis stays unbounded instead of becoming a huge-but-finite number).
        // It captures nothing, so it compiles to a plain static method with no allocation.
        fun Int.minusBounded(used: Int): Int =
            if (this == Constraints.Infinity) this else (this - used).coerceAtLeast(0)

        // ── Shared setup ──
        // Children never see the min constraints, because the container decides how to honour
        // them (by centring the content inside a bigger box). Constraints is a value class,
        // so copy() doesn't allocate.
        val loose = constraints.copy(minWidth = 0, minHeight = 0)

        // ── Case 1: single child ──
        // Measure it, size the container around it (respecting min/max), and centre it.
        // Orientation and spacing are irrelevant with one child.
        if (measurables.size == 1) {
            val p = measurables[0].measure(loose)
            val width = constraints.constrainWidth(p.width)
            val height = constraints.constrainHeight(p.height)
            return layout(width, height) {
                p.placeRelative((width - p.width) / 2, (height - p.height) / 2)
            }
        }

        // ── Case 2: icon + label ──

        // Step 1: resolve axis and gap (px) once.
        val horizontal = orientation == Orientation.Horizontal
        val gap = spacing.roundToPx()

        // Step 2: measure. The icon takes its natural size first. The label then gets the
        // leftover main-axis space, so a long label shrinks/ellipsizes instead of pushing
        // the icon out of the button.
        val icon = measurables[0].measure(loose)
        val label = measurables[1].measure(
            if (horizontal) loose.copy(maxWidth = loose.maxWidth.minusBounded(icon.width + gap))
            else loose.copy(maxHeight = loose.maxHeight.minusBounded(icon.height + gap))
        )

        // Step 3: content block size. Sum (+ gap) on the main axis, max on the cross axis.
        val contentWidth =
            if (horizontal) icon.width + gap + label.width else maxOf(icon.width, label.width)
        val contentHeight =
            if (horizontal) maxOf(icon.height, label.height) else icon.height + gap + label.height

        // Step 4: container size. The content block coerced into the incoming constraints.
        // It may be larger than the content when min constraints apply.
        val width = constraints.constrainWidth(contentWidth)
        val height = constraints.constrainHeight(contentHeight)

        Log.d("ButtonMeasure", "constraints=$constraints icon=${icon.width}x${icon.height} label=${label.width}x${label.height}")

        return layout(width, height) {
            // Step 5: centre the content block inside the container.
            val startX = (width - contentWidth) / 2
            val startY = (height - contentHeight) / 2

            // Step 6: place children, each centred on the cross axis.
            // placeRelative flips x in RTL, so icon and label swap sides automatically.
            // Vertical stacking has no horizontal direction, so it is unaffected.
            if (horizontal) {
                icon.placeRelative(startX, startY + (contentHeight - icon.height) / 2)
                label.placeRelative(
                    startX + icon.width + gap,
                    startY + (contentHeight - label.height) / 2,
                )
            } else {
                icon.placeRelative(startX + (contentWidth - icon.width) / 2, startY)
                label.placeRelative(
                    startX + (contentWidth - label.width) / 2,
                    startY + icon.height + gap,
                )
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