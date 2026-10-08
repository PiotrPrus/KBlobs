package dev.piotrprus.kblobs.sample.web

/** Kotlin excerpts of the library shown on the page, as written in kblobs/src/commonMain. */
internal object Snippets {
    val buildPath = """
        // buildPath(): one pass over every sample
        for (i in 0 until n) {
            val wobble = wobbleAt(values, segments, i.toFloat() / n - offset)
            val d = displacement(wobble, minPx, maxPx, intensity)
            val x = samples.x[i] + samples.normalX[i] * d
            val y = samples.y[i] + samples.normalY[i] * d
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    """.trimIndent()

    val wobbleAt = """
        private fun wobbleAt(values: FloatArray, count: Int, position: Float): Float {
            if (count == 1) return values[0]
            val lap = position - floor(position)      // keep only 0..1
            val s = lap * count                       // e.g. 2.4 = 40% from point 2 to 3
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
    """.trimIndent()

    val displacement = """
        private fun displacement(wobble: Float, min: Float, max: Float, intensity: Float): Float {
            val mid = (min + max) / 2f
            val half = (max - min) / 2f
            return mid + half * wobble * intensity
        }
    """.trimIndent()

    val layerMotion = """
        private class LayerMotion(seed: Int, val segments: Int) {
            init {
                val random = Random(seed)
                for (i in 0 until segments) {
                    slowFrequency[i] = TWO_PI * 0.25f * (0.7f + 0.6f * random.nextFloat())
                    fastFrequency[i] = TWO_PI * 0.55f * (0.7f + 0.6f * random.nextFloat())
                    slowPhase[i] = TWO_PI * random.nextFloat()
                    fastPhase[i] = TWO_PI * random.nextFloat()
                }
            }

            fun values(time: Float, out: FloatArray) {
                for (i in 0 until segments) {
                    out[i] = 0.65f * sin(slowFrequency[i] * time + slowPhase[i]) +
                        0.35f * sin(fastFrequency[i] * time + fastPhase[i])
                }
            }
        }
    """.trimIndent()

    val rotation = """
        val offset = (layer.rotation + spinDegrees) / 360f
        // in BlobClock.advance():
        spins[i] = (spins[i] + dt * layer.spin * speed) % 360f
    """.trimIndent()
}
