package com.zs.compose.foundation.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.zs.compose.foundation.requirePrecondition
import kotlin.math.roundToInt

/** Fixed-point scale: this many packed units represent 1.dp (quarter-dp precision). */
private const val UNIT_SCALE = 4

/** 16-bit mask for extracting one packed edge from [EdgeInsets.packed]. */
private const val COMPONENT_MASK = 0xFFFFuL

/**
 * Converts a non-negative [Dp] into its packed fixed-point form (see [UNIT_SCALE]),
 * clamped to whatever 16 bits can hold (0..65535 units, i.e. 0..16383.75.dp).
 * Clamping here — rather than throwing — keeps overflow a silent, harmless
 * ceiling instead of a crash, since it's an artifact of the bit width, not a
 * real semantic limit.
 */
private fun Dp.toPackedComponent(): UInt =
    (value * UNIT_SCALE).roundToInt().coerceIn(0, 0xFFFF).toUInt()

/** Unpacks the 16-bit edge value at [shift] back into a [Dp]. */
private fun EdgeInsets.componentAt(shift: Int): Dp =
    (((packed shr shift) and COMPONENT_MASK).toInt() / UNIT_SCALE.toFloat()).dp

/**
 * Edge padding for the four sides of a layout, packed into a single [ULong]
 * instead of four separate [Dp] fields — keeps this a zero-allocation value
 * class that can be stored/compared as one primitive.
 *
 * Bit layout (MSB → LSB), 16 bits per edge, quarter-dp precision, max
 * ~16383.75.dp per edge:
 * ```
 * [63......48][47........32][31.......16][15.......0]
 *   start(16)      top(16)      end(16)     bottom(16)
 * ```
 */
@JvmInline
@Immutable
value class EdgeInsets (val packed: ULong) {

    /** Inset for the leading (LTR: left) edge. */
    val start: Dp get() = componentAt(48)

    /** Inset for the top edge. */
    val top: Dp get() = componentAt(32)

    /** Inset for the trailing (LTR: right) edge. */
    val end: Dp get() = componentAt(16)

    /** Inset for the bottom edge. */
    val bottom: Dp get() = componentAt(0)

    @Stable
    override fun toString() = "EdgeInsets(start=$start, top=$top, end=$end, bottom=$bottom)"
}

/** Left inset, resolving [start]/[end] against [layoutDirection]. */
@Stable
fun EdgeInsets.calculateLeftPaddingInset(layoutDirection: LayoutDirection) =
    if (layoutDirection == LayoutDirection.Ltr) start else end

/** Top inset (direction-independent). */
@Stable
fun EdgeInsets.calculateTopInset() = top

/** Right inset, resolving [start]/[end] against [layoutDirection]. */
@Stable
fun EdgeInsets.calculateRightInset(layoutDirection: LayoutDirection) =
    if (layoutDirection == LayoutDirection.Ltr) end else start

/** Bottom inset (direction-independent). */
@Stable
fun EdgeInsets.calculateBottomInset() = bottom

/** Adds two [EdgeInsets] together, edge by edge. */
@Stable
operator fun EdgeInsets.plus(other: EdgeInsets) =
    EdgeInsets(start + other.start, top + other.top, end + other.end, bottom + other.bottom)

/**
 * Subtracts [other] from this [EdgeInsets], edge by edge. Each result is
 * clamped to 0 — insets can't be negative (see the factory function below).
 */
@Stable
operator fun EdgeInsets.minus(other: EdgeInsets) =
    EdgeInsets(
        (start - other.start).coerceAtLeast(0.dp),
        (top - other.top).coerceAtLeast(0.dp),
        (end - other.end).coerceAtLeast(0.dp),
        (bottom - other.bottom).coerceAtLeast(0.dp),
    )

/**
 * Creates an [EdgeInsets] with explicit start, top, end, and bottom padding.
 *
 * @throws IllegalArgumentException if any parameter is negative — that's a
 * caller bug, not something to silently clamp. (Values *above* the packable
 * max are clamped in [toPackedComponent], since that ceiling is only a
 * storage-width artifact, not a real invariant.)
 */
@Stable
fun EdgeInsets(start: Dp = 0.dp, top: Dp = 0.dp, end: Dp = 0.dp, bottom: Dp = 0.dp): EdgeInsets {
    requirePrecondition(start.value >= 0f && top.value >= 0f && end.value >= 0f && bottom.value >= 0f) {
        "EdgeInsets must be non-negative"
    }
    return EdgeInsets(
        (start.toPackedComponent().toULong() shl 48) or
                (top.toPackedComponent().toULong() shl 32) or
                (end.toPackedComponent().toULong() shl 16) or
                bottom.toPackedComponent().toULong()
    )
}

/** Creates an [EdgeInsets] with symmetric horizontal and vertical padding. */
@Stable
fun EdgeInsets(horizontal: Dp = 0.dp, vertical: Dp = 0.dp) =
    EdgeInsets(start = horizontal, end = horizontal, top = vertical, bottom = vertical)

/** Creates an [EdgeInsets] with all four edges set to [all]. */
@Stable
fun EdgeInsets(all: Dp) = EdgeInsets(start = all, end = all, top = all, bottom = all)