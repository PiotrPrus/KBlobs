package dev.piotrprus.kblobs.sample.web

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.kblobs.Blob
import dev.piotrprus.kblobs.BlobLayer
import dev.piotrprus.kblobs.BlobPresets
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** A plain float the frame loop advances; draws read it after reading the frame counter. */
internal class FloatBox(var value: Float)

/**
 * A lit-sphere brush sized to whatever it fills, so it can be handed to a real [BlobLayer] as
 * its `brush` without knowing the blob's size up front.
 */
internal fun sphereBrush(pal: BlobPalette): Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val r = min(size.width, size.height) / 2f
        return RadialGradientShader(
            center = Offset(size.width / 2f - r * 0.35f, size.height / 2f - r * 0.4f),
            radius = r * 1.55f,
            colors = listOf(pal.light, pal.body, pal.dark),
            colorStops = listOf(0f, 0.55f, 1f),
        )
    }

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = pal.hashCode()
}

@Composable
private fun captionStyle(size: Float = 11f): TextStyle = Type.mono(size).copy(color = Ink.muted, lineHeight = (size * 1.2f).sp)

// ---------- hero ----------

@Composable
internal fun Hero() {
    StepSection(
        leftWeight = 1.1f,
        rightWeight = 1f,
        topRule = false,
        modifier = Modifier.padding(top = 56.dp, bottom = 8.dp),
        left = { HeroText() },
        right = { HeroBlob() },
    )
}

@Composable
private fun HeroText() {
    Eyebrow("KBlobs · how the math works")
    H1("A circle whose edge breathes")
    Para(
        "A blob is a circle with its edge pushed in and out. A handful of numbers that rise and fall over time decide how far each part of the edge moves. Every frame, KBlobs repeats the same four steps:",
        style = Type.lede(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "Walk around the shape and drop evenly spaced points on its edge.",
            "Work out the current value of each control point, from two sine waves.",
            "Draw a smooth curve through those values, all the way round the lap.",
            "Push every edge point out or in along its own arrow, by that curve.",
        ).forEachIndexed { i, step ->
            androidx.compose.foundation.layout.Row {
                Text("${i + 1}.", style = Type.mono(15f).copy(color = Ink.gold, lineHeight = 27.sp), modifier = Modifier.size(width = 26.dp, height = 27.dp))
                Para(step)
            }
        }
    }
    Para("Each step below has a live canvas. Drag the sliders and watch only that one idea change.", style = Type.body().copy(color = Ink.muted))
}

/** The hero's blob: the real library composable with the real preset, nothing mirrored. */
@Composable
private fun HeroBlob() {
    val pal = Palettes[0]
    val layers = remember(pal) {
        val preset = BlobPresets.aura(body = pal.body, aura = pal.aura, layers = 3, reach = 34.dp)
        preset.dropLast(1) + preset.last().copy(brush = sphereBrush(pal))
    }
    Well(1f) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Blob(
                layers = layers,
                modifier = Modifier.size(maxWidth * 0.54f),
                animate = sectionRunning(),
            )
        }
    }
}

// ---------- step 1 ----------

@Composable
internal fun StepOutline() {
    var n by remember { mutableIntStateOf(24) }
    var push by remember { mutableIntStateOf(0) }
    StepSection(
        left = {
            Eyebrow("Step 1 · the outline")
            H2("Points on the edge, each with an arrow")
            Para("KBlobs never draws a circle directly. It walks around the shape's outline and drops `n` points at equal distances, where `n = segments × 16`, kept between 120 and 1024.")
            Para("Every point also gets a *normal*: a unit-length arrow pointing straight out of the edge. On a circle that is simply the direction from the centre. Later, every point slides along its own arrow, and that sliding is the whole blob effect.")
            Para("Because it samples any `Shape` outline, the same code bends a rounded rectangle just as well as a circle.", style = Type.body().copy(color = Ink.muted))
            CodeBlock(Snippets.buildPath)
        },
        right = {
            Well(1f, tag = "samples + normals") {
                Canvas(Modifier.fillMaxSize()) {
                    val c = center
                    val r = minSide * 0.3f
                    ring(c, r, Ink.line, floatArrayOf(4f, 5f))
                    val d = push * density
                    val pts = ArrayList<Offset>(n)
                    for (i in 0 until n) {
                        val a = TWO_PI * i / n - TWO_PI / 4f
                        val nx = cos(a)
                        val ny = sin(a)
                        val p = Offset(c.x + nx * r, c.y + ny * r)
                        line(p, p + Offset(nx, ny) * (26f * density), Ink.cool.copy(alpha = 0.8f), 1.5f)
                        pts += p + Offset(nx, ny) * d
                        dot(p, 2.5f, Ink.muted)
                    }
                    val path = androidx.compose.ui.graphics.Path()
                    pts.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                    path.close()
                    drawPath(path, Ink.gold, style = Stroke(2f * density))
                    pts.forEach { dot(it, 3.5f, Ink.gold) }
                }
            }
            ControlsGrid(
                { IntSlider("samples n", n, 6..160) { n = it } },
                { IntSlider("push every point by", push, -40..40, ::dpLabel) { push = it } },
            )
            Note("With the same push for every point, the circle just grows or shrinks. A blob needs a different push at different places on the edge. That is step 2.")
        },
    )
}

// ---------- steps 2 and 3 share hand-set control values ----------

@Stable
internal class HandValues {
    var values by mutableStateOf(floatArrayOf(0.8f, -0.4f, 0.3f, -0.9f, 0.5f, -0.2f))
    var smooth by mutableStateOf(true)
    var dragging by mutableIntStateOf(-1)

    fun resize(k: Int) {
        val old = values
        values = FloatArray(k) { i -> if (i < old.size) old[i] else 0f }
    }

    fun set(k: Int, v: Float) {
        values = values.copyOf().also { it[k] = v }
    }
}

/** Pure version of controlPoint for hit testing outside a DrawScope. */
private fun controlPointPx(c: Offset, r: Float, k: Int, count: Int, value: Float, minPx: Float, maxPx: Float): Offset {
    val a = TWO_PI * k / count - TWO_PI / 4f
    val d = displacement(value, minPx, maxPx, 1f)
    return Offset(c.x + cos(a) * (r + d), c.y + sin(a) * (r + d))
}

@Composable
internal fun StepControlPoints(hand: HandValues) {
    val measurer = rememberTextMeasurer()
    val labelStyle = captionStyle(10f)
    val numberStyle = Type.mono(10f).copy(color = Ink.page, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp)
    StepSection(
        left = {
            Eyebrow("Step 2 · control points")
            H2("A few numbers spread around the lap")
            Para("A layer has `segments` control points, spaced evenly around the edge. Position on the edge is measured as a fraction of one lap, from 0 to 1. Each control point holds one number between −1 and 1: −1 pulls the edge fully in, +1 pushes it fully out.")
            Para("Edge points that sit *between* two control points need an in-between value. Straight lines would leave a corner at every control point. KBlobs uses a Catmull-Rom curve instead. To find the value between control points `p1` and `p2`, it also looks at their outer neighbours `p0` and `p3`, with `t` going from 0 at `p1` to 1 at `p2`:")
            Formula(
                """
                v(t) = ½ · ( 2·p1
                       + (p2 − p0)·t
                       + (2·p0 − 5·p1 + 4·p2 − p3)·t²
                       + (3·p1 − p0 − 3·p2 + p3)·t³ )
                """.trimIndent(),
            )
            Para("Two properties make it right for blobs. At `t = 0` the curve is exactly `p1` and at `t = 1` exactly `p2`, so it passes through every control point. And the slope at each point is `(next − previous) / 2`, the same from both sides, so there are no corners.")
            Para("The indices wrap with `% count`, so after the last control point comes the first one again and the lap closes without a seam.")
            CodeBlock(Snippets.wobbleAt)
        },
        right = {
            Well(1f, tag = "drag the gold dots in and out") {
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerHoverIcon(PointerIcon.Hand)
                        .pointerInput(hand) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val r = min(size.width, size.height) * 0.3f
                                val ampPx = 40.dp.toPx()
                                val values = hand.values
                                var best = -1
                                var bestD = 28.dp.toPx()
                                values.forEachIndexed { k, v ->
                                    val p = controlPointPx(c, r, k, values.size, v, -ampPx, ampPx)
                                    val dd = (p - down.position).getDistance()
                                    if (dd < bestD) {
                                        bestD = dd
                                        best = k
                                    }
                                }
                                if (best < 0) return@awaitEachGesture
                                down.consume()
                                hand.dragging = best
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    change.consume()
                                    val dist = (change.position - c).getDistance()
                                    if (best < hand.values.size) hand.set(best, ((dist - r) / ampPx).coerceIn(-1f, 1f))
                                }
                                hand.dragging = -1
                            }
                        },
                ) {
                    val c = center
                    val r = minSide * 0.3f
                    val o = EdgeOptions(-40f, 40f, smooth = hand.smooth)
                    ring(c, r, Ink.line, floatArrayOf(4f, 5f))
                    ring(c, r - 40f * density, Ink.cool.copy(alpha = 0.25f), floatArrayOf(2f, 6f))
                    ring(c, r + 40f * density, Ink.cool.copy(alpha = 0.25f), floatArrayOf(2f, 6f))
                    val v = hand.values
                    val edge = circleEdge(c, r, v, o)
                    drawPath(edge, Ink.gold.copy(alpha = 0.18f))
                    drawPath(edge, Ink.gold, style = Stroke(2f * density))
                    v.forEachIndexed { k, value ->
                        val (p, a) = controlPoint(c, r, k, v.size, value, o)
                        val inner = r - 40f * density
                        val outer = r + 40f * density
                        line(
                            Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
                            Offset(c.x + cos(a) * outer, c.y + sin(a) * outer),
                            Ink.muted.copy(alpha = 0.5f),
                            1f,
                        )
                        dot(p, if (k == hand.dragging) 9f else 7f, Ink.goldSoft)
                        val layout = measurer.measure((k + 1).toString(), numberStyle)
                        drawText(layout, topLeft = p - Offset(layout.size.width / 2f, layout.size.height / 2f))
                    }
                }
            }
            Well(3f, tag = "the same lap, unrolled: 0 → 1") {
                Canvas(Modifier.fillMaxSize()) {
                    unrolledLap(hand.values, measurer, labelStyle, smooth = hand.smooth)
                }
            }
            ControlsGrid({ IntSlider("segments", hand.values.size, 1..12) { hand.resize(it) } })
            ControlRow {
                CheckToggle("Catmull-Rom (off = straight lines)", hand.smooth) { hand.smooth = it }
                PillButton("Random values", ghost = true) {
                    hand.values = FloatArray(hand.values.size) { Random.nextFloat() * 2f - 1f }
                }
                PillButton("All zero", ghost = true) { hand.values = FloatArray(hand.values.size) }
            }
            Note("Nothing moves yet. The dots are fixed numbers you set by hand. In the strip, the right edge continues into the left edge, because the lap is a loop.")
        },
    )
}

@Composable
internal fun StepAmplitude(hand: HandValues) {
    var minDp by remember { mutableIntStateOf(-20) }
    var maxDp by remember { mutableIntStateOf(30) }
    var intensity by remember { mutableIntStateOf(100) }
    val measurer = rememberTextMeasurer()
    val caption = captionStyle()
    StepSection(
        left = {
            Eyebrow("Step 3 · from wobble to pixels")
            H2("Amplitude turns −1..1 into dp")
            Para("The curve gives a wobble between −1 and 1. `displacement()` maps it onto the layer's `amplitude` range: −1 lands on the range's start, +1 on its end.")
            Formula("mid  = (min + max) / 2\nhalf = (max − min) / 2\nd    = mid + half · wobble · intensity")
            Para("`amplitude = (-4).dp..4.dp` gives mid 0 and half 4: the edge moves ±4 dp around the original circle. `(-4).dp..24.dp` gives mid 10 and half 14: the blob is on average 10 dp bigger and swings 14 dp each way. That is how the outer aura layers reach out.")
            Para("`intensity` scales only the swing, not the mid. At 0 you get a plain circle grown by `mid`. An app can feed the microphone level in here so the blob reacts to your voice.")
            CodeBlock(Snippets.displacement)
        },
        right = {
            Well(1f, tag = "dashed rings: radius + min, radius + max") {
                Canvas(Modifier.fillMaxSize()) {
                    val c = center
                    val r = minSide * 0.28f
                    val mn = minDp.toFloat()
                    val mx = maxOf(maxDp, minDp).toFloat()
                    ring(c, r, Ink.line, floatArrayOf(4f, 5f))
                    ring(c, r + mn * density, Ink.cool.copy(alpha = 0.6f), floatArrayOf(3f, 5f))
                    ring(c, r + mx * density, Ink.cool.copy(alpha = 0.6f), floatArrayOf(3f, 5f))
                    ring(c, r + (mn + mx) / 2f * density, Ink.goldSoft.copy(alpha = 0.35f), floatArrayOf(1f, 4f))
                    val o = EdgeOptions(mn, mx, intensity / 100f, smooth = hand.smooth)
                    val edge = circleEdge(c, r, hand.values, o)
                    drawPath(edge, Ink.gold.copy(alpha = 0.2f))
                    drawPath(edge, Ink.gold, style = Stroke(2f * density))
                    hand.values.forEachIndexed { k, v -> dot(controlPoint(c, r, k, hand.values.size, v, o).first, 5f, Ink.goldSoft) }
                    val text = "mid ${((mn + mx) / 2f).roundToInt()}dp · half ${((mx - mn) / 2f).roundToInt()}dp"
                    val layout = measurer.measure(text, caption)
                    drawText(layout, topLeft = Offset(12f * density, size.height - 12f * density - layout.size.height))
                }
            }
            ControlsGrid(
                { IntSlider("amplitude start", minDp, -60..40, ::dpLabel) { minDp = it } },
                { IntSlider("amplitude end", maxDp, -40..80, ::dpLabel) { maxDp = it } },
                { IntSlider("intensity", intensity, 0..150, ::x100) { intensity = it } },
            )
            Note("The control values here are the ones you set in step 2. Go back and drag them to see both ideas together.")
        },
    )
}

// ---------- step 4 ----------

@Composable
internal fun StepMotion() {
    var segments by remember { mutableIntStateOf(6) }
    var tempo by remember { mutableIntStateOf(100) }
    var mix by remember { mutableIntStateOf(65) }
    var seed by remember { mutableIntStateOf(defaultSeed(0)) }
    val motion = remember(seed, segments) { LayerMotion(seed, segments) }
    val values = remember(segments) { FloatArray(segments) }
    val time = remember { FloatBox(3f) }
    StepSection(
        left = {
            Eyebrow("Step 4 · making it move")
            H2("Two sine waves per control point")
            Para("Now the control values stop being hand-set numbers. Every frame, each one is the sum of a slow sine and a faster one:")
            Formula("value = 0.65 · sin(slowFreq · time + slowPhase)\n      + 0.35 · sin(fastFreq · time + fastPhase)")
            Para("The weights add up to 1 (0.65 + 0.35), so the value can never leave −1..1. The slow wave does the big breathing, the fast one adds a smaller ripple on top so the motion doesn't look like a metronome.")
            Para("Each control point gets its own frequencies and phases from a seeded `Random`. The slow one is about 0.25 cycles per second and the fast one about 0.55, each nudged by a random factor between 0.7 and 1.3. Because no two points share a rhythm, the overall shape practically never repeats. The seed makes it the same blob on every launch.")
            CodeBlock(Snippets.layerMotion)
            Para("`time` is not the wall clock. Each frame the layer's clock moves forward by `dt × tempo × speed`, with `dt` capped at 0.05 s. So changing `tempo` mid-animation changes speed smoothly instead of jumping, and a dropped frame doesn't cause a leap.")
        },
        right = {
            val frame = rememberFrameLoop(sectionRunning()) { dt -> time.value += dt * tempo / 100f }
            Well(1f, tag = "control point 1 is the ringed one") {
                Canvas(Modifier.fillMaxSize()) {
                    frame.value
                    motion.values(time.value, values, mix / 100f)
                    val c = center
                    val r = minSide * 0.3f
                    val o = EdgeOptions(-24f, 24f)
                    ring(c, r, Ink.line, floatArrayOf(4f, 5f))
                    drawPath(circleEdge(c, r, values, o), Ink.gold.copy(alpha = 0.85f))
                    values.forEachIndexed { i, v ->
                        val p = controlPoint(c, r, i, values.size, v, o).first
                        dot(p, if (i == 0) 7f else 4f, Ink.goldSoft, if (i == 0) Ink.page else null)
                    }
                }
            }
            Well(3f, tag = "point 1, last 8 seconds: slow · fast · sum") {
                Canvas(Modifier.fillMaxSize()) {
                    frame.value
                    val w = size.width
                    val h = size.height
                    val padX = 18f * density
                    val top = 30f * density
                    val bottom = h - 14f * density
                    val span = 8f
                    fun yOf(v: Float) = top + (1f - (v + 1f) / 2f) * (bottom - top)
                    fun xOf(s: Float) = padX + (s / span) * (w - 2 * padX)
                    listOf(-1f, 0f, 1f).forEach { v -> drawLine(Ink.line, Offset(padX, yOf(v)), Offset(w - padX, yOf(v)), density) }
                    val m = mix / 100f
                    val tp = maxOf(tempo / 100f, 0.0001f)
                    val series: List<Triple<(Float) -> Float, Color, Float>> = listOf(
                        Triple({ t -> m * motion.slow(0, t) }, Ink.cool.copy(alpha = 0.8f), 1.2f),
                        Triple({ t -> (1f - m) * motion.fast(0, t) }, Ink.muted.copy(alpha = 0.7f), 1.2f),
                        Triple({ t -> m * motion.slow(0, t) + (1f - m) * motion.fast(0, t) }, Ink.gold, 2.2f),
                    )
                    series.forEach { (f, color, width) ->
                        val path = androidx.compose.ui.graphics.Path()
                        for (i in 0..200) {
                            val sec = i / 200f * span
                            val t = time.value - (span - sec) * tp
                            val v = f(t)
                            if (i == 0) path.moveTo(xOf(sec), yOf(v)) else path.lineTo(xOf(sec), yOf(v))
                        }
                        drawPath(path, color, style = Stroke(width * density))
                    }
                    dot(Offset(xOf(span), yOf(m * motion.slow(0, time.value) + (1f - m) * motion.fast(0, time.value))), 5f, Ink.goldSoft)
                }
            }
            ControlsGrid(
                { IntSlider("segments", segments, 2..12) { segments = it } },
                { IntSlider("tempo", tempo, 0..300, ::x100) { tempo = it } },
                { IntSlider("slow weight", mix, 0..100, ::x100) { mix = it } },
            )
            ControlRow {
                PillButton("New seed", ghost = true) { seed = Random.nextInt(100_000) }
                Text("seed = $seed", style = Type.note())
            }
        },
    )
}

// ---------- step 5 ----------

@Composable
internal fun StepRotation() {
    var rotation by remember { mutableIntStateOf(0) }
    var spinRate by remember { mutableIntStateOf(20) }
    var freeze by remember { mutableStateOf(false) }
    val motion = remember { LayerMotion(defaultSeed(1), 5) }
    val values = remember { FloatArray(5) }
    val time = remember { FloatBox(1f) }
    val spin = remember { FloatBox(0f) }
    val measurer = rememberTextMeasurer()
    val caption = captionStyle()
    StepSection(
        left = {
            Eyebrow("Step 5 · rotation and spin")
            H2("Sliding the pattern around the lap")
            Para("Before looking up the wobble, KBlobs subtracts an offset from the position: `wobbleAt(values, i / n − offset)`. Subtracting moves every control point forward by the same fraction of a lap, so the whole bump pattern turns. The circle itself never rotates; only where the bumps sit changes.")
            Formula("offset = (rotation + spinDegrees) / 360")
            Para("`rotation` is a fixed starting angle. `spin` is degrees per second, added to the layer's spin clock every frame and kept inside one lap with `% 360`. Without spin the bumps breathe in place; with spin they also travel round the edge. The aura preset gives each layer a different `rotation` (47° apart) so layers don't bulge in the same spots.")
            CodeBlock(Snippets.rotation)
        },
        right = {
            val frame = rememberFrameLoop(sectionRunning()) { dt ->
                if (!freeze) time.value += dt
                spin.value = (spin.value + dt * spinRate) % 360f
            }
            Well(1f, tag = "the line marks control point 1") {
                Canvas(Modifier.fillMaxSize()) {
                    frame.value
                    motion.values(time.value, values)
                    val c = center
                    val r = minSide * 0.3f
                    val o = EdgeOptions(-22f, 22f, rotation = rotation.toFloat(), spin = spin.value)
                    drawPath(circleEdge(c, r, values, o), Ink.gold.copy(alpha = 0.85f))
                    val p = controlPoint(c, r, 0, 5, values[0], o).first
                    line(c, p, Ink.goldSoft, 1.5f)
                    for (i in 1 until 5) dot(controlPoint(c, r, i, 5, values[i], o).first, 3.5f, Ink.page)
                    dot(p, 6f, Ink.goldSoft, Ink.page)
                    val off = ((o.offset % 1f) + 1f) % 1f
                    val layout = measurer.measure("offset = ${off.fixed(3)} lap", caption)
                    drawText(layout, topLeft = Offset(12f * density, size.height - 12f * density - layout.size.height))
                }
            }
            ControlsGrid(
                { IntSlider("rotation", rotation, 0..360, { "$it°" }) { rotation = it } },
                { IntSlider("spin (°/s)", spinRate, -90..90) { spinRate = it } },
            )
            CheckToggle("freeze the wobble (only spin moves)", freeze) { freeze = it }
        },
    )
}

// ---------- step 6 ----------

@Composable
internal fun StepLayers() {
    var count by remember { mutableIntStateOf(3) }
    var reach by remember { mutableIntStateOf(36) }
    var blur by remember { mutableIntStateOf(0) }
    var outline by remember { mutableStateOf(false) }
    val pal = Palettes[0]
    // The real preset, drawn by the real Blob. Only colours, blur and outline mode are adjusted.
    val layers = remember(count, reach, blur, outline) {
        val preset = BlobPresets.aura(body = pal.body, aura = pal.aura, layers = count, reach = reach.dp)
        preset.mapIndexed { i, layer ->
            val body = i == preset.lastIndex
            when {
                outline && body -> layer.copy(strokeWidth = 1.5.dp)
                outline -> layer.copy(alpha = 0.9f, strokeWidth = 1.5.dp)
                body -> layer.copy(brush = sphereBrush(pal))
                blur > 0 -> layer.copy(blur = blur.dp)
                else -> layer
            }
        }
    }
    val rows = remember(count, reach) {
        auraRows(count, reach.toFloat()).mapIndexed { i, l ->
            listOf(i.toString(), l.alpha.fixed(2), l.segments.toString(), "-4..${l.max.roundToInt()}dp", l.tempo.fixed(2), "${l.rotation.roundToInt()}°")
        } + listOf(listOf("$count · body", "1.00", "6", "0..0dp", "1.00", "0°"))
    }
    StepSection(
        left = {
            Eyebrow("Step 6 · layers")
            H2("Several blobs on top of each other")
            Para("`Blob` draws a list of `BlobLayer`s, first one at the bottom. Each layer runs everything above on its own: its own seed (`0x5EED + index × 7919` unless you pass one), segments, amplitude, tempo, rotation, colour and alpha. Translucent layers that move out of step are what make the surface look alive.")
            Para("`BlobPresets.aura` builds that stack for you: translucent moving layers, outermost first, and a still, solid body on top with `amplitude = 0.dp..0.dp`. For layer `i` of `L`:")
            Formula(
                """
                depth     = 1 − i / L          (1 = outermost)
                alpha     = 0.12 + 0.14 · (1 − depth)
                segments  = 5 + 2·i
                amplitude = −4.dp .. reach · depth
                tempo     = 0.7 + 0.35·i
                rotation  = 47° · i
                """.trimIndent(),
            )
            Para("Outer layers are fainter, have fewer bumps, reach further and move slower. Inner layers are denser, bumpier and quicker. A `blur` above 0 draws that layer into its own graphics layer with a `BlurEffect`, padded so the blur isn't clipped.")
            MonoTable(listOf("layer", "alpha", "segments", "amplitude", "tempo", "rotation"), rows)
        },
        right = {
            Well(1f) {
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Blob(
                        layers = layers,
                        modifier = Modifier.size(maxWidth * 0.48f),
                        animate = sectionRunning(),
                    )
                }
            }
            ControlsGrid(
                { IntSlider("layers", count, 0..6) { count = it } },
                { IntSlider("reach", reach, 0..80, ::dpLabel) { reach = it } },
                { IntSlider("blur on aura layers", blur, 0..24, ::dpLabel) { blur = it } },
            )
            CheckToggle("draw layers as outlines", outline) { outline = it }
            Note("This one is the library's own `Blob` and `BlobPresets.aura`, not a re-drawing.")
        },
    )
}
