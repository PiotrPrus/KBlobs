package dev.piotrprus.kblobs.sample.web

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import dev.piotrprus.kblobs.BlobLayer
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Teaching copies of the KBlobs internals.
//
// The library keeps its math private (dev.piotrprus.kblobs.internal and the private BlobClock in
// Blob.kt), so the explainer mirrors it here to draw control points, normals and graphs. Every
// function below is a line-for-line copy of the library version, with a comment naming the
// original. The only additions are the knobs the explainer needs: a slow-wave weight for step 4
// and a straight-line switch for step 2. With their default values the results are identical to
// the library, which is why the playground's overlays line up with the real Blob drawn under them.

internal const val TWO_PI = (2 * PI).toFloat()

/** Mirror of `internal class LayerMotion` in kblobs/internal/Wobble.kt. Same seeded Kotlin Random. */
internal class LayerMotion(seed: Int, val segments: Int) {
    val slowFrequency = FloatArray(segments)
    val fastFrequency = FloatArray(segments)
    val slowPhase = FloatArray(segments)
    val fastPhase = FloatArray(segments)

    init {
        val random = Random(seed)
        for (i in 0 until segments) {
            slowFrequency[i] = TWO_PI * 0.25f * (0.7f + 0.6f * random.nextFloat())
            fastFrequency[i] = TWO_PI * 0.55f * (0.7f + 0.6f * random.nextFloat())
            slowPhase[i] = TWO_PI * random.nextFloat()
            fastPhase[i] = TWO_PI * random.nextFloat()
        }
    }

    fun slow(i: Int, time: Float): Float = sin(slowFrequency[i] * time + slowPhase[i])
    fun fast(i: Int, time: Float): Float = sin(fastFrequency[i] * time + fastPhase[i])

    /** Library: `0.65f * slow + 0.35f * fast`. [slowWeight] is the explainer's step-4 knob. */
    fun values(time: Float, out: FloatArray, slowWeight: Float = 0.65f) {
        for (i in 0 until segments) {
            out[i] = if (slowWeight == 0.65f) {
                0.65f * slow(i, time) + 0.35f * fast(i, time)
            } else {
                slowWeight * slow(i, time) + (1f - slowWeight) * fast(i, time)
            }
        }
    }
}

/** Mirror of `internal fun wobbleAt` in kblobs/internal/Wobble.kt. [smooth] false is step 2's straight lines. */
internal fun wobbleAt(values: FloatArray, count: Int, position: Float, smooth: Boolean = true): Float {
    if (count == 1) return values[0]
    val lap = position - floor(position)
    val s = lap * count
    val i = floor(s).toInt().coerceAtMost(count - 1)
    val t = s - i
    val p0 = values[(i - 1 + count) % count]
    val p1 = values[i]
    val p2 = values[(i + 1) % count]
    val p3 = values[(i + 2) % count]
    if (!smooth) return p1 + (p2 - p1) * t
    val t2 = t * t
    val t3 = t2 * t
    val v = 0.5f * (
        2f * p1 +
            (p2 - p0) * t +
            (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
            (3f * p1 - p0 - 3f * p2 + p3) * t3
        )
    return v.coerceIn(-1f, 1f)
}

/** Mirror of `internal fun displacement` in kblobs/internal/Wobble.kt. */
internal fun displacement(wobble: Float, min: Float, max: Float, intensity: Float): Float {
    val mid = (min + max) / 2f
    val half = (max - min) / 2f
    return mid + half * wobble * intensity
}

/** Mirror of `private fun defaultSeed` in Blob.kt. */
internal fun defaultSeed(index: Int): Int = 0x5EED + index * 7919

/** Mirror of the sample count picked in Blob.kt: `(maxSegments * 16).coerceIn(120, 1024)`. */
internal fun sampleCount(maxSegments: Int): Int = (maxSegments * 16).coerceIn(120, 1024)

/** Mirror of `internal class OutlineSamples` in kblobs/internal/OutlineSamples.kt. */
internal class OutlineSamples(
    val x: FloatArray,
    val y: FloatArray,
    val normalX: FloatArray,
    val normalY: FloatArray,
) {
    val count: Int get() = x.size
}

/** Mirror of `internal fun sampleOutline` in kblobs/internal/OutlineSamples.kt. */
internal fun sampleOutline(path: Path, count: Int): OutlineSamples? {
    val measure = PathMeasure()
    measure.setPath(path, forceClosed = true)
    val length = measure.length
    if (length <= 0f || count < 3) return null
    val x = FloatArray(count)
    val y = FloatArray(count)
    for (i in 0 until count) {
        val p = measure.getPosition(length * i / count)
        x[i] = p.x
        y[i] = p.y
    }
    return fromPoints(x, y)
}

/** Mirror of `internal fun fromPoints` in kblobs/internal/OutlineSamples.kt. */
internal fun fromPoints(x: FloatArray, y: FloatArray): OutlineSamples {
    val count = x.size
    var nx = FloatArray(count)
    var ny = FloatArray(count)
    var area = 0f
    for (i in 0 until count) {
        val j = (i + 1) % count
        area += x[i] * y[j] - x[j] * y[i]
    }
    val sign = if (area >= 0f) 1f else -1f
    for (i in 0 until count) {
        val prev = (i - 1 + count) % count
        val next = (i + 1) % count
        nx[i] = sign * (y[next] - y[prev])
        ny[i] = sign * -(x[next] - x[prev])
    }
    repeat(2) {
        val sx = FloatArray(count)
        val sy = FloatArray(count)
        for (i in 0 until count) {
            val prev = (i - 1 + count) % count
            val next = (i + 1) % count
            sx[i] = nx[prev] + 2f * nx[i] + nx[next]
            sy[i] = ny[prev] + 2f * ny[i] + ny[next]
        }
        nx = sx
        ny = sy
    }
    for (i in 0 until count) {
        val len = sqrt(nx[i] * nx[i] + ny[i] * ny[i])
        if (len > 0f) {
            nx[i] /= len
            ny[i] /= len
        }
    }
    return OutlineSamples(x, y, nx, ny)
}

/**
 * Mirror of `private class BlobClock` in Blob.kt: per-layer time and spin, integrated frame by
 * frame. Advanced from the same frames and with the same arithmetic as the Blob it shadows, it
 * holds the same numbers.
 */
internal class MirrorClock {
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
            spins[i] = (spins[i] + dt * layer.spin * speed) % 360f
        }
    }
}

/** One moving layer of `BlobPresets.aura`, the same formulas, as numbers for the step 6 table. */
internal data class AuraRow(
    val alpha: Float,
    val segments: Int,
    val min: Float,
    val max: Float,
    val tempo: Float,
    val rotation: Float,
)

/** Mirror of the loop in `BlobPresets.aura`. */
internal fun auraRows(layers: Int, reach: Float): List<AuraRow> = (0 until layers).map { i ->
    val depth = if (layers == 1) 1f else 1f - i.toFloat() / layers
    AuraRow(
        alpha = 0.12f + 0.14f * (1f - depth),
        segments = 5 + i * 2,
        min = -4f,
        max = reach * depth,
        tempo = 0.7f + 0.35f * i,
        rotation = i * 47f,
    )
}

/** A teaching layer for the hand-drawn canvases: its own motion and clock. */
internal class TeachLayer(
    seed: Int,
    val segments: Int,
    var time: Float,
) {
    val motion = LayerMotion(seed, segments)
    val values = FloatArray(segments)
}
