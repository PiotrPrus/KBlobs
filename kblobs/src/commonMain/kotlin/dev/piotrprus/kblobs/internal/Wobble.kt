package dev.piotrprus.kblobs.internal

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private const val TWO_PI = (2 * PI).toFloat()

/**
 * The motion of one layer's control points: two sines per point with seeded frequencies and
 * phases. Neighbouring points are unrelated, which is what makes the outline read as organic
 * rather than as a rotating pattern.
 */
internal class LayerMotion(seed: Int, val segments: Int) {
    private val slowFrequency = FloatArray(segments)
    private val fastFrequency = FloatArray(segments)
    private val slowPhase = FloatArray(segments)
    private val fastPhase = FloatArray(segments)

    init {
        val random = Random(seed)
        for (i in 0 until segments) {
            slowFrequency[i] = TWO_PI * 0.25f * (0.7f + 0.6f * random.nextFloat())
            fastFrequency[i] = TWO_PI * 0.55f * (0.7f + 0.6f * random.nextFloat())
            slowPhase[i] = TWO_PI * random.nextFloat()
            fastPhase[i] = TWO_PI * random.nextFloat()
        }
    }

    /** Writes each control point's value in `-1..1` at layer time [time] into [out]. */
    fun values(time: Float, out: FloatArray) {
        for (i in 0 until segments) {
            out[i] = 0.65f * sin(slowFrequency[i] * time + slowPhase[i]) +
                0.35f * sin(fastFrequency[i] * time + fastPhase[i])
        }
    }
}

/**
 * The wobble at [position] along the outline, where `0..1` is one lap, given the control point
 * [values] spread evenly around it. Closed Catmull-Rom through the points, clamped to `-1..1` so
 * the overshoot never breaks the layer's amplitude range.
 */
internal fun wobbleAt(values: FloatArray, count: Int, position: Float): Float {
    if (count == 1) return values[0]
    val lap = position - floor(position)
    val s = lap * count
    val i = floor(s).toInt().coerceAtMost(count - 1)
    val t = s - i
    val p0 = values[(i - 1 + count) % count]
    val p1 = values[i]
    val p2 = values[(i + 1) % count]
    val p3 = values[(i + 2) % count]
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

/**
 * Maps a wobble in `-1..1` to a displacement between [min] and [max]. [intensity] scales only the
 * swing around the middle of the range, so an intensity of 0 leaves the layer still at the
 * range's midpoint instead of collapsing it onto the shape.
 */
internal fun displacement(wobble: Float, min: Float, max: Float, intensity: Float): Float {
    val mid = (min + max) / 2f
    val half = (max - min) / 2f
    return mid + half * wobble * intensity
}
