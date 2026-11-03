package com.zs.compose.foundation.util

import android.os.Build
import androidx.compose.ui.Modifier

// Represents some common utility funs

/**
 * Indicates whether the application is currently running within a preview environment (e.g., Android Studio's layout preview).
 *
 * Unlike `View.isInEditMode` or `LocalInspectionMode`, this check can be performed from outside of Compose's composition.
 * This is crucial for scenarios where ripple behavior needs to be determined before entering the composition phase.
 *
 * [source](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material/material-ripple/src/androidMain/kotlin/androidx/compose/material/ripple/Ripple.android.kt;l=81;bpv=0)
 */
internal val IsRunningInPreview = Build.DEVICE == "layoutlib"

/**
 * Conditionally applies another [Modifier] if the given [condition] is true.
 *
 * @param condition The condition to evaluate.
 * @param other The [Modifier] to apply if the condition is true.
 * @return This [Modifier] if the condition is false, otherwise this [Modifier] combined with [other].
 */
inline fun Modifier.thenIf(condition: Boolean, other: Modifier.() -> Modifier) =
    if (condition) this then Modifier.other() else this