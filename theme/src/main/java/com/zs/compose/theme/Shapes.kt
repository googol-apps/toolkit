package com.zs.compose.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * [AppTheme] surfaces can be displayed in different shapes. Shapes direct attention, identify
 * components, communicate state, and express brand.
 *
 * The shape scale defines the style of container corners, offering a range of roundedness from
 * square to fully circular.
 *
 * @param xSmall Slightly rounded; larger than [RectangleShape], smaller than [small].
 *   Suited to menus, snackbars, and text fields.
 * @param small Larger than [xSmall], smaller than [medium]. Suited to chips.
 * @param medium Larger than [small], smaller than [large]. Suited to cards and small FABs.
 * @param large Larger than [medium], smaller than [xLarge]. Suited to FABs and navigation drawers.
 * @param xLarge Larger than [large], smaller than [CircleShape]. Suited to large FABs.
 *
 * [Reference](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Shapes.kt;bpv=0)
 */
// Source: androidx.compose.material3.Shapes (values follow the M3 shape scale).
@Immutable
class Shapes(
    val xSmall: CornerBasedShape = RoundedCornerShape(4.0.dp),
    val small: CornerBasedShape = RoundedCornerShape(8.0.dp),
    val medium: CornerBasedShape = RoundedCornerShape(12.0.dp),
    val large: CornerBasedShape = RoundedCornerShape(16.0.dp),
    val xLarge: CornerBasedShape = RoundedCornerShape(28.0.dp),
) {
    /** Returns a copy of this Shapes, optionally overriding some of the values. */
    fun copy(
        xSmall: CornerBasedShape = this.xSmall,
        small: CornerBasedShape = this.small,
        medium: CornerBasedShape = this.medium,
        large: CornerBasedShape = this.large,
        xLarge: CornerBasedShape = this.xLarge,
    ): Shapes = Shapes(
        xSmall = xSmall,
        small = small,
        medium = medium,
        large = large,
        xLarge = xLarge,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Shapes) return false
        if (xSmall != other.xSmall) return false
        if (small != other.small) return false
        if (medium != other.medium) return false
        if (large != other.large) return false
        if (xLarge != other.xLarge) return false
        return true
    }

    override fun hashCode(): Int {
        var result = xSmall.hashCode()
        result = 31 * result + small.hashCode()
        result = 31 * result + medium.hashCode()
        result = 31 * result + large.hashCode()
        result = 31 * result + xLarge.hashCode()
        return result
    }

    override fun toString(): String {
        return "Shapes(extraSmall=$xSmall, small=$small, medium=$medium, large=$large, extraLarge=$xLarge)"
    }
}