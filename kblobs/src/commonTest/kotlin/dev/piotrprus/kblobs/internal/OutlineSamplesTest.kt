package dev.piotrprus.kblobs.internal

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

class OutlineSamplesTest {

    private fun circle(count: Int, clockwiseOnScreen: Boolean): Pair<FloatArray, FloatArray> {
        val x = FloatArray(count)
        val y = FloatArray(count)
        for (i in 0 until count) {
            val a = 2.0 * PI * i / count * if (clockwiseOnScreen) 1 else -1
            x[i] = (100 + 50 * cos(a)).toFloat()
            y[i] = (100 + 50 * sin(a)).toFloat()
        }
        return x to y
    }

    @Test
    fun normalsPointOutwardWhicheverWayThePointsRun() {
        for (clockwise in listOf(true, false)) {
            val (x, y) = circle(64, clockwise)
            val s = fromPoints(x, y)
            for (i in 0 until s.count) {
                val rx = (x[i] - 100) / 50
                val ry = (y[i] - 100) / 50
                assertEquals(rx, s.normalX[i], 1e-3f, "x at $i, clockwise=$clockwise")
                assertEquals(ry, s.normalY[i], 1e-3f, "y at $i, clockwise=$clockwise")
            }
        }
    }

    @Test
    fun normalsAreUnitLength() {
        // A square: the corners are where tangent-based normals would break.
        val x = floatArrayOf(0f, 10f, 20f, 20f, 20f, 10f, 0f, 0f)
        val y = floatArrayOf(0f, 0f, 0f, 10f, 20f, 20f, 20f, 10f)
        val s = fromPoints(x, y)
        for (i in 0 until s.count) {
            val len = s.normalX[i] * s.normalX[i] + s.normalY[i] * s.normalY[i]
            assertEquals(1f, len, 1e-4f)
        }
        // The top-left corner's normal points up and left, out of the square.
        assertEquals(true, s.normalX[0] < 0f && s.normalY[0] < 0f)
    }
}
