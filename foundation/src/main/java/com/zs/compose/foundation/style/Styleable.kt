package com.zs.compose.foundation.style

import androidx.compose.foundation.style.CommonStyle
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.StyleState
import androidx.compose.foundation.style.styleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.foundation.interaction.InteractionSource as Interactions

/**
 * Alias for the style type consumed by [styleable].
 */
@OptIn(ExperimentalFoundationStyleApi::class)
typealias Style = CommonStyle

@OptIn(ExperimentalFoundationStyleApi::class)
private class BlockStyleableElement(
    val state: StyleState? = null,
    val style: Style,
    val interactions: Interactions? = null
) : ModifierNodeElement<BlockStyleableNode>() {

    // Lifecycle
    override fun create() = BlockStyleableNode(state, style, interactions)
    override fun update(node: BlockStyleableNode) = node.invalidate(state, style, interactions)

    // Tooling
    override fun InspectorInfo.inspectableProperties() {
        name = "styleable"
        properties["state"] = state
        properties["style"] = style
        properties["interactions"] = interactions
    }

    // Identity
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockStyleableElement

        if (state != other.state) return false
        if (style != other.style) return false
        if (interactions != other.interactions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + style.hashCode()
        result = 31 * result + interactions.hashCode()
        return result
    }
}

/**
 * Applies a fixed [style] to this modifier chain.
 *
 * Use this overload when styling does not depend on a [StyleState].
 *
 * @see styleable for the state-aware overload.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
fun Modifier.styleable(interactions: Interactions? = null, style: Style) =
    this then BlockStyleableElement(
        style = style,
        interactions = interactions
    )

/**
 * Applies a [style] resolved from the provided [state].
 */
@OptIn(ExperimentalFoundationStyleApi::class)
fun Modifier.styleable(state: StyleState, style: Style) = this then BlockStyleableElement(
    state = state,
    style = style
)


