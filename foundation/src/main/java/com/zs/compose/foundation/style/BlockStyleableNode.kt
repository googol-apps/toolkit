package com.zs.compose.foundation.style

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.foundation.style.Fill
import androidx.compose.foundation.style.MutableStyleState
import androidx.compose.foundation.style.StyleResolver
import androidx.compose.foundation.style.StyleState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.setOutline
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.semantics.shape
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.takeOrElse
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.interaction.InteractionSource as Interactions
import androidx.compose.ui.node.DelegatingNode as Node
import androidx.compose.ui.node.DrawModifierNode as Draw
import androidx.compose.ui.node.LayoutModifierNode as Layout
import androidx.compose.ui.node.ObserverModifierNode as Observer
import androidx.compose.ui.node.SemanticsModifierNode as SemanticsNode
import androidx.compose.ui.semantics.SemanticsPropertyReceiver as Semantics

private const val TAG = "BlockStyleNode"

/**
 * Fallback [StyleState] used when neither a [StyleState] nor an [Interactions]
 * source is supplied. It never changes, so the style resolves once and stays static.
 *
 * CHECK: `MutableStyleState(null)` must be a valid constructor call in your API version.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
private val NoOpStyleState = MutableStyleState(null)

/**
 * Reflective handle to Compose's internal StyleResolverNode constructor.
 *
 * WARNING: This relies on internal APIs and may break between Compose releases.
 * Release builds also need the R8 keep rule from the header of this file.
 * TODO - Replace with a public API if one becomes available.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
private val ResolverNode by lazy {
    // Resolve the internal constructor once and reuse it for all node instances.
    // Reflection is isolated here so any future Compose incompatibility fails
    // during initialization with a clear error message.
    // [CHANGED #6] runCatching + getOrElse keeps the original exception as the cause.
    // Before, `error(...)` threw a new exception and the real reason was lost.
    runCatching {
        val clazz = Class.forName(
            "androidx.compose.foundation.style.StyleResolverNode"
        )

        // Expected constructor:
        // StyleResolverNode(StyleResolver, CoroutineScope)
        val constructor = clazz.getDeclaredConstructor(
            StyleResolver::class.java,
            CoroutineScope::class.java
        )

        // Internal constructor is not publicly accessible.
        constructor.isAccessible = true

        constructor
    }.getOrElse { cause ->
        throw IllegalStateException(
            "Failed to resolve Compose internal StyleResolverNode constructor.",
            cause
        )
    }
}

// Each flag names the work a change needs, not the property that changed.
private const val DIRTY_MEASURE = 1 shl 0     // padding or size changed, so re-measure and re-place
private const val DIRTY_DRAW = 1 shl 1        // fills, border or shape changed, so redraw
private const val DIRTY_SEMANTICS = 1 shl 2   // exposed semantics changed, so rebuild them
private const val DIRTY_LAYER = 1 shl 3       // layer values (elevation, ...) changed, so re-apply them // [CHANGED #10] comment was cut off

/**
 * Node behind [BlockStyleableElement]. It owns the [StyleResolver], the framework node that
 * loads the resolved style.
 *
 * @param state Caller-provided state. Takes priority over [interactions] if both are set.
 * @param style The style to resolve.
 * @param interactions Interaction source used to build a [StyleState] when [state] is null.
 */
@OptIn(ExperimentalFoundationStyleApi::class)
internal class BlockStyleableNode(
    private var state: StyleState? = null,
    private var style: Style,
    private var interactions: Interactions? = null,
) : Node(), Layout, Draw, Observer, SemanticsNode {
    override val shouldAutoInvalidate: Boolean = false

    // TODO: Remove this workaround once the draw invalidation bug is fixed.
    //
    // Workaround: invalidateDraw() doesn't re-run draw() for this node, because its content is
    // placed with a layer. Reading this snapshot state registers a dependency, so each bump from
    // the observer re-records this draw.
    //
    // We intentionally create and reuse the GraphicsLayer in measure rather than
    // in draw to avoid re-recording the layer on every draw pass. This also helps
    // avoid extra draw operations triggered by indications.
    //
    // In the future, features such as outer shadows or outlines with gaps may
    // require switching to GraphicsLayer recording during draw instead.
    private var drawWorkaroundInvalidationTick by mutableIntStateOf(0)

    // the size of the child placeable.
    private var dimensions: Size = Size.Unspecified

    // Currently delegated framework resolver node, or null before attach / after detach.
    private lateinit var layer: GraphicsLayer
    private var loader: DelegatableNode? = null
    private var resolver: StyleResolver? = null
    private var outline: Outline? = null

    // properties
    private var padding = PaddingValues.Zero
    private var shape = RectangleShape
    private var background: Fill = Fill.None
    private var foreground: Fill = Fill.None
    private var elevation: Dp = Dp.Unspecified
    private var border: BorderStroke? = null

    /**
     * Read block for [observeReads]. Any state read inside it triggers
     * [onObservedReadsChanged].
     */
    private val observer: () -> Unit = reads@{
        Log.d(TAG, "observer: ")
        val resolver = resolver ?: return@reads
        // Phase 1: read every property first, so a change to any of them re-runs this block.
        val newBackground = resolver[StyleBackgroundProperty]
        val newForeground = resolver[StyleForegroundProperty]
        val newShape = resolver[StyleShapeProperty]
        val newPadding = resolver[StylePaddingProperty]
        val newElevation = resolver[StyleElevationProperty]
        val newBorder = resolver[StyleBorderProperty]

        // Phase 2: compare against the stored values and collect the effects needed.
        var flags = 0
        if (background != newBackground || foreground != newForeground || border != newBorder) flags =
            flags or DIRTY_DRAW
        if (shape != newShape) flags = flags or DIRTY_SEMANTICS // draw: the fills follow the shape
        if (padding != newPadding) flags = flags or DIRTY_MEASURE
        if (elevation != newElevation) flags = flags or DIRTY_LAYER

        // Commit the new values.
        background = newBackground
        foreground = newForeground
        padding = newPadding
        elevation = newElevation
        border = newBorder
        if (shape != newShape) {
            shape = newShape
            invalidateOutline()
        }

        if (flags == 0) return@reads
        Log.d(TAG, "observer: invalidating")

        // Phase 3: apply. Layer first, because the draw pass reads what it builds.
        if (flags and DIRTY_DRAW != 0) invalidateDraw()
        if (flags and DIRTY_LAYER != 0) invalidateLayer()
        if (flags and DIRTY_MEASURE != 0) invalidateMeasurement()
        if (flags and DIRTY_SEMANTICS != 0) invalidateSemantics()
    }

    // cached properties
    private var cachedNoiseEffectBrush: ShaderBrush? = null
    private var cachedBorderStroke: Stroke? = null

    /**
     * Rebuilds the layer's outline from the current [shape], [childSize], layout direction and
     * density, and pushes it to [layer] (shadow shape and clip).
     *
     * Call it whenever one of those inputs changes: after a size change in `measure`, after a
     * shape change in the observer, and from [onDensityChange] and [onLayoutDirectionChange].
     * It is a no-op until the node is attached and measured.
     */
    private fun invalidateOutline() {
        // Guard: nothing to build before the first measure, or after detach, when the layer
        // is already released.
        val size = dimensions
        if (!isAttached || size.isUnspecified) return

        // Build from the live values.
        val outline = shape.createOutline(size, requireLayoutDirection(), requireDensity())

        // Apply to the layer. This is the only place that writes the layer's outline.
        layer.setOutline(outline)
        // Clip only for non-rectangular shapes. A rectangular layer is already bounded by its size.
        layer.clip = shape != RectangleShape
        this.outline = outline
    }

    /**
     * Workaround for the framework's draw invalidation, which doesn't re-run [draw] for this
     * node because its content is placed with a layer.
     *
     * Bumps [drawWorkaroundInvalidationTick], which [draw] reads, so the change re-records the
     * draw. Remove it together with the tick and the read in [draw] once the framework bug is fixed.
     */
    private fun invalidateDraw() {
        // Bump without read observation. `++` reads the value before writing it, and this is
        // called from inside `observeReads(observer)`. Tracking that read would make the observer
        // re-run on every bump with nothing changed. Int overflow wraps around on its own.
        Snapshot.withoutReadObservation { ++drawWorkaroundInvalidationTick }
    }

    /**
     * Pushes the cheap layer properties (shadow elevation) onto [layer].
     *
     * These are applied on the render side, so changing them re-renders the layer without
     * re-recording its content. Anything that changes *what* is drawn belongs in the draw path.
     *
     * NOTE: [layer] wraps only the placed child (see [measure]), so the shadow and the clip
     * follow the child's bounds, not the padded box that the fills and border cover. If the
     * shadow should surround the whole box, the layer would have to wrap it instead.
     */
    private fun invalidateLayer() {
        Log.d(TAG, "invalidateLayer: ")
        layer.shadowElevation = with(requireDensity()) { elevation.takeOrElse() { 0.dp }.toPx() }
    }

    override fun onLayoutDirectionChange() = invalidateOutline()
    override fun onDensityChange() = invalidateOutline()
    override fun onObservedReadsChanged() = observeReads(observer)

    //
    override fun Semantics.applySemantics() {
        this.shape = this@BlockStyleableNode.shape
    }

    /**
     * Reconciles the latest element inputs with this node's current state.
     *
     * Inputs are processed in dependency order:
     *
     * interactions -> indication -> state -> style -> resolver/loader
     *
     * Once a change is detected, all downstream steps are re-executed because
     * they may depend on values updated earlier in the chain.
     *
     * When the node is not attached yet, inputs are only stored. The actual
     * resolver and loader graph is created after attachment, when a
     * [CoroutineScope] becomes available.
     *
     * @param force Forces a full reconciliation and rebuild even when all
     * inputs are equal to the currently stored values. Used by [onAttach] to
     * perform the initial setup after pre-attachment input storage.
     */
    fun invalidate(
        state: StyleState?,
        style: Style,
        interactions: Interactions?,
        force: Boolean = false
    ) {
        // Before attach there is no coroutineScope, so only store the inputs.
        // onAttach calls this again with force = true to do the actual build.
        if (!isAttached) {
            this.state = state
            this.style = style
            this.interactions = interactions
            return
        }

        var changed = force

        // Interactions: a new source invalidates the indication, the fallback state
        // and the resolver.
        if (changed || this.interactions != interactions) {
            this.interactions = interactions
            changed = true
        }

        // State and style: store the caller's values untouched, so `this.state` is never
        // overwritten by the fallback. A different style needs a new resolver but not a
        // new indication.
        if (changed || state != this.state) {
            this.state = state
            changed = true
        }
        if (changed || style != this.style) {
            this.style = style
            changed = true
        }

        // Nothing changed, so keep the current resolver and loader.
        if (!changed) return
        val oldResolver = resolver

        // Effective state, in priority order: caller state, then a state built from
        // interactions, then the shared static NoOpStyleState. Non-null by construction,
        // so the resolver never receives a null state.
        // TODO: A new MutableStyleState is created on every rebuild. Check whether it
        // should be remembered per interactions instance.
        val resolved: StyleState = state
            ?: interactions?.let { MutableStyleState(it) }
            ?: NoOpStyleState

        // Resolver and loader: build the new pair, swap it in, then release the old
        // resolver last so there is no gap without an active one.
        // CHECK: ResolverNode is reflective. The constructor must match
        // (StyleResolver, CoroutineScope) exactly, or newInstance throws at runtime.
        // TODO: Wrap the reflective call in a try/catch with a clear error message.
        val newResolver = StyleResolver(style, resolved)
        resolver = newResolver
        loader?.let { undelegate(it) }
        loader = delegate(
            ResolverNode.newInstance(newResolver, coroutineScope) as DelegatableNode
        )
        oldResolver?.onAbandoned()

        // Start tracking the reads done in `observer` for the new resolver.
        observeReads(observer)
        Log.d(TAG, "update: ")
    }

    // onAttach - Builds everything for the first time. Forced, because the stored inputs.
    override fun onAttach() {
        Log.d(TAG, "onAttach: ")
        // Graphics layer: create it before anything can write to it.
        layer = requireGraphicsContext().createGraphicsLayer()
        // Style resolution, indication and observation: forced, because the stored inputs
        // already equal the passed ones, so the change checks would skip the build.
        invalidate(state, style, interactions, force = true)
    }

    // onDetach Lifecycle - Destroy and release resources.
    override fun onDetach() {
        Log.d(TAG, "onDetach: ")

        // Graphics layer: hand it back to the graphics context. Nothing below may touch
        // `layer` again until onAttach creates a new one.
        requireGraphicsContext().releaseGraphicsLayer(layer = layer)

        // Style resolution: remove the framework loader node, then release the resolver so it
        // stops observing state. `sync` rebuilds both on the next attach.
        loader?.let { undelegate(it) }
        loader = null
        resolver?.onAbandoned()
        resolver = null

        // Layout state: forget the last known size, density and direction, so nothing is
        // built from stale values after a re-attach.
        dimensions = Size.Unspecified

        // Cached outline: clear it last. Assigning null rebuilds the outline, but with the
        // size above already unspecified the setter returns early. That is what keeps it from
        // touching the layer that was just released. Do not move this line above the size reset.
        outline = null // clear outline as well.
    }

    // draw
    override fun ContentDrawScope.draw() {
        Log.d(TAG, "draw: ")
        // CHECK: confirm the color still animates in a minified release build. R8 shouldn't remove
        // a snapshot state getter, but I haven't verified that.
        @Suppress("UNUSED_VARIABLE")
        val tick = drawWorkaroundInvalidationTick
        // Background: behind the content. Nothing is drawn for Fill.None.
        // CHECK: `drawRect` ignores the shape. Use `drawOutline` with the cached outline if
        // the fills should follow it.
        when (val bg = background) {
            is Fill.Color -> drawRect(bg.color)
            is Fill.Brush -> drawRect(bg.brush)
            else -> {}
        }

        // Content: the element's own drawing, in between the two fills.
        this@draw.drawContent()

        // Draw Border/Outline
        // We draw the border last. We use a Stroke style with double thickness (borderWidthPx * 2)
        // because drawOutline centered on the path clips half of the stroke width inside/outside.
        val border = border
        if (border != null) {
            val widthPx = border.width.toPx()
            val style = cachedBorderStroke ?: Stroke(widthPx * 2)
            val outline = outline
            if (outline != null)
                drawOutline(outline, style = style, brush = border.brush)
        }

        // Foreground: on top of the content. Same rules as the background.
        when (val bg = foreground) {
            is Fill.Color -> drawRect(bg.color)
            is Fill.Brush -> drawRect(bg.brush)
            else -> {}
        }
    }

    // here update padding, size, default size. etc.
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        Log.d(TAG, "measure: ")
        // Resolve and validate content padding.
        val lPadding = padding.calculateLeftPadding(layoutDirection)
        val tPadding = padding.calculateTopPadding()
        val rPadding = padding.calculateRightPadding(layoutDirection)
        val bPadding = padding.calculateBottomPadding()

        require((lPadding >= 0.dp) and (tPadding >= 0.dp) and (rPadding >= 0.dp) and (bPadding >= 0.dp)) {
            "Padding must be non-negative"
        }

        // Convert padding to pixels and derive the total horizontal/vertical space
        // reserved around the child.
        val roundedLeftPadding = lPadding.roundToPx()
        val horizontal = roundedLeftPadding + rPadding.roundToPx()

        val roundedTopPadding = tPadding.roundToPx()
        val vertical = roundedTopPadding + bPadding.roundToPx()

        // Measure the child within the remaining space after padding is applied.
        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))

        // Compute the final size including padding and constrain it to the
        // incoming parent constraints.
        val width = constraints.constrainWidth(placeable.width + horizontal)
        val height = constraints.constrainHeight(placeable.height + vertical)

        // Sync the inputs the cached outline depends on (size, density, layout direction).
        // If any of them changed, the outline is stale, so invalidate it and let it be
        // rebuilt.
        val size = Size(placeable.width.toFloat(), placeable.height.toFloat())
        if (size != dimensions) {
            dimensions = size
            invalidateOutline()
        }

        // Position the child and attach the reusable graphics layer.
        return layout(width, height) {
            Log.d(TAG, "placement: ")
            placeable.placeWithLayer(roundedLeftPadding, roundedTopPadding, layer = layer)
        }
    }
}