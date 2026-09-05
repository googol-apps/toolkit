package com.zs.compose.foundation.unit

import androidx.annotation.IntRange
import androidx.compose.runtime.Stable

/**
 * A spherical coordinate (azimuth, polar angle, distance) packed into a single [ULong].
 *
 * Bit layout (MSB → LSB), 64 bits total:
 * ```
 * [63........55][54.......47][46......................0]
 *   azimuth (9)    polar (8)         distance (47)
 * ```
 * Packing everything into one `ULong` keeps this a zero-allocation value class —
 * no boxing, and it can be stored/compared as a single primitive (e.g. in a
 * [ULongArray] or as a map key) instead of three separate fields.
 *
 * Use the [Spherical] factory function below to construct instances from raw
 * degrees/distance — it normalizes out-of-range input rather than throwing
 * (see its KDoc). The primary constructor here just wraps an already-packed
 * value, e.g. for deserialization.
 */
@JvmInline
value class Spherical(val packed: ULong) {

    /**
     * Azimuthal angle in degrees, `0..359`.
     *
     * Stored in bits 55–63 (9 bits, capacity 0..511 — more than enough
     * headroom for 0..359). Azimuth is cyclic: 0° and 360° are the same
     * direction, so the factory function below folds 360 down to 0 —
     * this getter will never return 360.
     */
    val azimuth: Int
        get() = ((packed shr 55) and 0x1FFu).toInt()

    /**
     * Polar (inclination) angle in degrees, `0..180`.
     *
     * Stored in bits 47–54 (8 bits, capacity 0..255). Unlike azimuth this
     * is *not* cyclic — 0 and 180 are the two poles, not wraparound points.
     *
     * NOTE: at a pole (`polar == 0` or `polar == 180`), azimuth is not
     * geometrically meaningful — every azimuth value at a pole represents
     * the same physical point. [equals] is bitwise (via [packed]) and does
     * NOT account for this: two Sphericals at a pole with different azimuth
     * will compare unequal even though they represent the same direction.
     */
    val polar: Int
        get() = ((packed shr 47) and 0xFFu).toInt()

    /**
     * Radial distance, `0..MAX_DISTANCE`.
     *
     * Stored in bits 0–46 (47 bits, capacity ~1.4 × 10¹⁴) — the remaining
     * bits after azimuth and polar are given as much precision as possible,
     * since distance is the field most likely to need a large range.
     */
    val distance: Long
        get() = (packed and ((1uL shl 47) - 1uL)).toLong()

    /** Destructuring support: `val (az, pol, dist) = spherical`. */
    operator fun component1() = azimuth
    operator fun component2() = polar
    operator fun component3() = distance

    /**
     * Returns a copy with the given fields replaced. Goes through the
     * [Spherical] factory function, so replacement values are normalized
     * the same way as in the original constructor (azimuth wraps, polar
     * and distance clamp).
     */
    fun copy(
        azimuth: Int = this.azimuth,
        polar: Int = this.polar,
        distance: Long = this.distance,
    ) = Spherical(azimuth, polar, distance)

    /** Human-readable form for logging/debugging — not the packed bits. */
    override fun toString() = "Spherical(azimuth=$azimuth, polar=$polar, distance=$distance)"

    companion object {
        /** A [Spherical] at the origin: azimuth 0, polar 0, distance 0. */
        val Zero = Spherical(0, 0, 0L)

        /** Largest value [distance] can hold given its 47-bit allocation. */
        const val MAX_DISTANCE = (1L shl 47) - 1L
    }
}

/**
 * Creates a [Spherical] from individual components. This constructor never
 * throws — out-of-range inputs are normalized instead of rejected:
 * - [azimuth] wraps cyclically into `0..359` (e.g. `370` → `10`, `-10` → `350`,
 *   `360` → `0`), since azimuth is a rotational quantity where wraparound is
 *   a valid, expected case (accumulated rotation, negative input, etc.).
 * - [polar] is clamped into `0..180`, since it is bounded rather than cyclic
 *   (0 and 180 are the two poles — "past 180" isn't a meaningful wraparound).
 * - [distance] is clamped into `0..Spherical.MAX_DISTANCE`, which also
 *   absorbs negative input by clamping it to 0.
 *
 * The `@IntRange` annotations below are lint/documentation hints for callers
 * only — they are not enforced here; passing an out-of-range value will be
 * silently normalized rather than flagged.
 *
 * @param azimuth angle around the vertical axis, in degrees.
 * @param polar angle from the vertical axis, in degrees.
 * @param distance radial distance from the origin.
 */
@Stable
fun Spherical(
    @IntRange(0, 360) azimuth: Int,
    @IntRange(0, 180) polar: Int,
    @IntRange(0) distance: Long,
): Spherical {
    val normalizedAzimuth = ((azimuth % 360) + 360) % 360
    val clampedPolar = polar.coerceIn(0, 180)
    val clampedDistance = distance.coerceIn(0L, Spherical.MAX_DISTANCE)

    // Promote each component to ULong *before* shifting — shifting an Int by
    // more than 31 bits wraps around (JVM semantics), which silently produces
    // wrong results if done on the Int values directly.
    return Spherical(
        (normalizedAzimuth.toULong() shl 55) or
                (clampedPolar.toULong() shl 47) or
                clampedDistance.toULong()
    )
}