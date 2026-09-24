package dev.piotrprus.kblobs.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WobbleTest {

    @Test
    fun passesThroughEveryControlPoint() {
        val values = floatArrayOf(0.2f, -0.5f, 0.9f, 0f)
        values.forEachIndexed { i, v ->
            assertEquals(v, wobbleAt(values, 4, i / 4f), 1e-5f)
        }
    }

    @Test
    fun isContinuousAcrossTheSeam() {
        val values = floatArrayOf(0.3f, -0.7f, 0.1f, 0.8f, -0.2f)
        assertEquals(wobbleAt(values, 5, 0.9999f), wobbleAt(values, 5, 0f), 1e-2f)
        assertEquals(wobbleAt(values, 5, 1.25f), wobbleAt(values, 5, 0.25f), 1e-5f)
        assertEquals(wobbleAt(values, 5, -0.25f), wobbleAt(values, 5, 0.75f), 1e-5f)
    }

    @Test
    fun neverLeavesTheUnitRange() {
        // Alternating extremes make Catmull-Rom overshoot the most.
        val values = floatArrayOf(1f, -1f, 1f, -1f, 1f, -1f)
        for (k in 0..600) {
            val w = wobbleAt(values, 6, k / 600f)
            assertTrue(w in -1f..1f, "wobble $w at ${k / 600f}")
        }
    }

    @Test
    fun oneSegmentMovesTheWholeOutlineTogether() {
        val values = floatArrayOf(0.4f)
        assertEquals(0.4f, wobbleAt(values, 1, 0.1f))
        assertEquals(0.4f, wobbleAt(values, 1, 0.7f))
    }

    @Test
    fun motionStaysInRangeAndIsSeeded() {
        val a = LayerMotion(seed = 7, segments = 8)
        val b = LayerMotion(seed = 7, segments = 8)
        val va = FloatArray(8)
        val vb = FloatArray(8)
        for (step in 0..200) {
            val t = step * 0.1f
            a.values(t, va)
            b.values(t, vb)
            assertTrue(va.contentEquals(vb))
            va.forEach { assertTrue(it in -1f..1f) }
        }
    }

    @Test
    fun displacementCoversTheRange() {
        assertEquals(-3f, displacement(-1f, -3f, 5f, 1f))
        assertEquals(5f, displacement(1f, -3f, 5f, 1f))
        assertEquals(1f, displacement(1f, -3f, 5f, 0f))
        assertEquals(3f, displacement(0.5f, -3f, 5f, 1f))
    }
}
