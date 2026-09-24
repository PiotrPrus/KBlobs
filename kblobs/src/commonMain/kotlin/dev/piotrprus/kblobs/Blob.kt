package dev.piotrprus.kblobs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.piotrprus.kblobs.internal.LayerMotion
import dev.piotrprus.kblobs.internal.OutlineSamples
import dev.piotrprus.kblobs.internal.displacement
import dev.piotrprus.kblobs.internal.sampleOutline
import dev.piotrprus.kblobs.internal.wobbleAt
import kotlin.math.ceil
import kotlin.math.max

/**
 * Draws [layers] as morphing outlines of [shape], the first layer at the bottom.
 *
 * The shape fills this composable's bounds. Layers that move outward draw outside those bounds,
 * like a shadow does, so give the blob padding or a smaller size than its container when the
 * outer layers must stay visible.
 *
 * Both lambdas are read every frame without recomposing, so they can follow fast-changing state
 * such as a microphone level.
 *
 * @param layers The layers to draw, bottom first.
 * @param shape Base outline. Any [Shape] works: [CircleShape], rounded corners, a `GenericShape`,
 *   or a Material 3 shape. Only the first contour of the shape's outline is used.
 * @param intensity Multiplier of every layer's swing around the middle of its amplitude range.
 *   0 holds each layer still at the midpoint, 1 uses the full range, above 1 overshoots it.
 * @param speed Multiplier of every layer's tempo and spin. Changes take effect smoothly, because
 *   the wobble advances by `speed` each frame instead of being scaled after the fact.
 * @param animate False stops the motion where it is, for example for reduced motion.
 */
@Composable
public fun Blob(
    layers: List<BlobLayer>,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    intensity: () -> Float = { 1f },
    speed: () -> Float = { 1f },
    animate: Boolean = true,
) {
    val clock = remember { BlobClock() }
    val latestLayers by rememberUpdatedState(layers)
    val latestSpeed by rememberUpdatedState(speed)

    LaunchedEffect(clock, animate) {
        if (!animate) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                // Cap the step so a dropped frame or a return from background does not jump.
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                clock.advance(dt, latestLayers, latestSpeed())
            }
        }
    }

    Spacer(
        modifier.drawWithCache {
            val maxSegments = layers.maxOfOrNull { it.segments } ?: 1
            val samples = sampleOutline(
                path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) },
                count = (maxSegments * 16).coerceIn(MIN_SAMPLES, MAX_SAMPLES),
            )
            val prepared = layers.mapIndexed { index, layer ->
                PreparedLayer(
                    layer = layer,
                    motion = LayerMotion(layer.seed ?: defaultSeed(index), layer.segments),
                    values = FloatArray(layer.segments),
                    path = Path(),
                    minPx = layer.amplitude.start.toPx(),
                    maxPx = layer.amplitude.endInclusive.toPx(),
                    blurPx = layer.blur.toPx(),
                    graphicsLayer = if (layer.blur.value > 0f) obtainGraphicsLayer() else null,
                )
            }

            onDrawBehind {
                // Reading the frame counter ties this draw to the clock: each tick redraws
                // without recomposing.
                clock.frame
                if (samples == null) return@onDrawBehind
                val swing = intensity()
                prepared.forEachIndexed { index, p ->
                    p.buildPath(samples, clock.time(index), clock.spin(index), swing)
                    drawPrepared(p)
                }
            }
        },
    )
}

private const val MIN_SAMPLES = 120
private const val MAX_SAMPLES = 1024

private fun defaultSeed(index: Int): Int = 0x5EED + index * 7919

private class PreparedLayer(
    val layer: BlobLayer,
    val motion: LayerMotion,
    val values: FloatArray,
    val path: Path,
    val minPx: Float,
    val maxPx: Float,
    val blurPx: Float,
    val graphicsLayer: GraphicsLayer?,
) {
    fun buildPath(samples: OutlineSamples, time: Float, spinDegrees: Float, intensity: Float) {
        motion.values(time, values)
        val offset = (layer.rotation + spinDegrees) / 360f
        path.rewind()
        val n = samples.count
        for (i in 0 until n) {
            val wobble = wobbleAt(values, layer.segments, i.toFloat() / n - offset)
            val d = displacement(wobble, minPx, maxPx, intensity)
            val x = samples.x[i] + samples.normalX[i] * d
            val y = samples.y[i] + samples.normalY[i] * d
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
}

private fun DrawScope.drawPrepared(p: PreparedLayer) {
    val layer = p.layer
    val graphicsLayer = p.graphicsLayer
    if (graphicsLayer == null) {
        drawLayerPath(p, blend = true)
        return
    }
    // A blurred layer is drawn into its own graphics layer, grown on every side so the outward
    // swing and the blur's falloff are not clipped at the composable's bounds.
    val pad = ceil(max(0f, max(p.maxPx, -p.minPx)) + p.blurPx * 3f).toInt()
    graphicsLayer.renderEffect = BlurEffect(p.blurPx, p.blurPx, TileMode.Decal)
    graphicsLayer.blendMode = layer.blendMode
    graphicsLayer.topLeft = IntOffset(-pad, -pad)
    graphicsLayer.record(
        density = this,
        layoutDirection = layoutDirection,
        size = IntSize(ceil(size.width).toInt() + 2 * pad, ceil(size.height).toInt() + 2 * pad),
    ) {
        translate(pad.toFloat(), pad.toFloat()) { drawLayerPath(p, blend = false) }
    }
    drawLayer(graphicsLayer)
}

private fun DrawScope.drawLayerPath(p: PreparedLayer, blend: Boolean) {
    val layer = p.layer
    val blendMode = if (blend) layer.blendMode else DrawScope.DefaultBlendMode
    val brush = layer.brush
    if (brush != null) {
        drawPath(p.path, brush, alpha = layer.alpha, style = layer.style, blendMode = blendMode)
    } else {
        drawPath(p.path, layer.color, alpha = layer.alpha, style = layer.style, blendMode = blendMode)
    }
}

/**
 * Per-layer time, advanced frame by frame. Integrating tempo and spin, instead of multiplying a
 * shared clock by them, keeps a layer continuous when its tempo changes mid-animation.
 */
private class BlobClock {
    var frame by mutableLongStateOf(0L)
        private set
    private var times = FloatArray(0)
    private var spins = FloatArray(0)

    fun time(index: Int): Float = times.getOrElse(index) { 0f }
    fun spin(index: Int): Float = spins.getOrElse(index) { 0f }

    fun advance(dt: Float, layers: List<BlobLayer>, speed: Float) {
        if (times.size < layers.size) {
            times = times.copyOf(layers.size)
            spins = spins.copyOf(layers.size)
        }
        layers.forEachIndexed { i, layer ->
            times[i] += dt * layer.tempo * speed
            // Kept in one lap so the float keeps its precision over a long session.
            spins[i] = (spins[i] + dt * layer.spin * speed) % 360f
        }
        frame++
    }
}
