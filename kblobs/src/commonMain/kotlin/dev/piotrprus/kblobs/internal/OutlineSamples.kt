package dev.piotrprus.kblobs.internal

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlin.math.sqrt

/**
 * A closed outline sampled at equal distances, with an outward unit normal at every point.
 * Built once per size and shape; every frame only moves these points along their normals.
 */
internal class OutlineSamples(
    val x: FloatArray,
    val y: FloatArray,
    val normalX: FloatArray,
    val normalY: FloatArray,
) {
    val count: Int get() = x.size
}

/**
 * Samples the first contour of [path] at [count] equally spaced points, or returns null when the
 * path is empty.
 */
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

/**
 * Normals come from the neighbouring samples, not from the path's tangent. A tangent jumps at a
 * sharp corner and would tear the moved outline open there; the neighbour difference turns
 * through the corner instead. Two smoothing passes round it off further.
 */
internal fun fromPoints(x: FloatArray, y: FloatArray): OutlineSamples {
    val count = x.size
    var nx = FloatArray(count)
    var ny = FloatArray(count)

    // Screen coordinates have y pointing down, so a positive shoelace area means the points run
    // clockwise on screen, and the outward normal of tangent (tx, ty) is (ty, -tx).
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
