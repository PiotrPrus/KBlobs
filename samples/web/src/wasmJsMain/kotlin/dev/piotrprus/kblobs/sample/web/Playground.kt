package dev.piotrprus.kblobs.sample.web

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.kblobs.Blob
import dev.piotrprus.kblobs.BlobLayer
import dev.piotrprus.kblobs.BlobPresets
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Everything at once. The blob is the real library [Blob]. The overlays (control points, normals,
 * amplitude rings) and the unrolled strip come from the mirrored math in BlobMath.kt, driven by a
 * [MirrorClock] that advances on the same frames as the Blob's own clock. Because the mirror uses
 * the same seeds, the same Kotlin `Random`, the same outline sampling and the same per-layer time,
 * the overlays sit exactly on the real outline.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Playground() {
    var segments by remember { mutableIntStateOf(7) }
    var minDp by remember { mutableIntStateOf(-6) }
    var maxDp by remember { mutableIntStateOf(10) }
    var intensity by remember { mutableIntStateOf(100) }
    var tempo by remember { mutableIntStateOf(100) }
    var spin by remember { mutableIntStateOf(0) }
    var auraCount by remember { mutableIntStateOf(3) }
    var reach by remember { mutableIntStateOf(30) }
    var blur by remember { mutableIntStateOf(4) }
    var palIndex by remember { mutableIntStateOf(0) }
    var showControl by remember { mutableStateOf(true) }
    var showNormals by remember { mutableStateOf(false) }
    var showBand by remember { mutableStateOf(true) }
    var copyStatus by remember { mutableStateOf("") }
    val pal = Palettes[palIndex]
    val topMax = maxOf(minDp, maxDp)

    val layers: List<BlobLayer> = remember(segments, minDp, topMax, tempo, spin, auraCount, reach, blur, pal) {
        val aura = BlobPresets.aura(body = pal.body, aura = pal.aura, layers = auraCount, reach = reach.dp).dropLast(1)
        aura.map { if (blur > 0) it.copy(blur = blur.dp) else it } + BlobLayer(
            brush = sphereBrush(pal),
            segments = segments,
            amplitude = minDp.dp..topMax.dp,
            tempo = tempo / 100f,
            spin = spin.toFloat(),
        )
    }
    val code = remember(layers, intensity, pal, blur) { kotlinFor(layers, pal, blur, intensity) }

    // The mirror of Blob's own clock. Same start (all zeros), same frames, same arithmetic.
    val running = sectionRunning()
    val clock = remember { MirrorClock() }
    val latestLayers by rememberUpdatedState(layers)
    val frame = remember { mutableLongStateOf(0L) }
    val topIndex = layers.lastIndex
    val top = layers.last()
    val topMotion = remember(topIndex, top.segments) { LayerMotion(top.seed ?: defaultSeed(topIndex), top.segments) }
    val topValues = remember(top.segments) { FloatArray(top.segments) }
    val maxSegments = layers.maxOf { it.segments }
    val measurer = rememberTextMeasurer()
    val labelStyle = Type.mono(10f).copy(color = Ink.muted, lineHeight = 12.sp)

    StepSection(
        leftWeight = 1.25f,
        rightWeight = 1f,
        left = {
            Well(1f) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val localDensity = LocalDensity.current
                    val side = constraints.maxWidth
                    // Integer pixel size and offset, shared by the Blob and the overlay canvas.
                    val blobPx = ((side * 0.52f).roundToInt() / 2) * 2
                    val originPx = (side - blobPx) / 2
                    val blobDp = with(localDensity) { blobPx.toDp() }
                    val samples = remember(blobPx, maxSegments, localDensity) {
                        val path = Path().apply {
                            addOutline(CircleShape.createOutline(Size(blobPx.toFloat(), blobPx.toFloat()), LayoutDirection.Ltr, localDensity))
                        }
                        sampleOutline(path, sampleCount(maxSegments))
                    }

                    Canvas(Modifier.fillMaxSize()) {
                        val c = Offset(originPx + blobPx / 2f, originPx + blobPx / 2f)
                        val r = blobPx / 2f
                        drawCircle(
                            Brush.radialGradient(
                                0f to pal.aura.copy(alpha = 0.10f),
                                0.25f to pal.aura.copy(alpha = 0.10f),
                                1f to pal.aura.copy(alpha = 0f),
                                center = c,
                                radius = r * 2f,
                            ),
                            radius = r * 2f,
                            center = c,
                        )
                    }
                    // Launched in the same composition as the Blob below, so both loops start on
                    // the same frame and integrate identical dt values.
                    LaunchedEffect(clock, running) {
                        if (!running) return@LaunchedEffect
                        var last = 0L
                        while (true) {
                            withFrameNanos { now ->
                                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                                last = now
                                clock.advance(dt, latestLayers, 1f)
                                frame.longValue++
                            }
                        }
                    }
                    Blob(
                        layers = layers,
                        intensity = { intensity / 100f },
                        animate = running,
                        modifier = Modifier.offset { IntOffset(originPx, originPx) }.size(blobDp),
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        frame.longValue
                        val s = samples ?: return@Canvas
                        topMotion.values(clock.time(topIndex), topValues)
                        val offset = (top.rotation + clock.spin(topIndex)) / 360f
                        val minPx = top.amplitude.start.toPx()
                        val maxPx = top.amplitude.endInclusive.toPx()
                        val swing = intensity / 100f
                        val n = s.count
                        translate(originPx.toFloat(), originPx.toFloat()) {
                            if (showBand) {
                                listOf(minPx, maxPx).forEach { d ->
                                    val ring = Path()
                                    for (i in 0 until n) {
                                        val x = s.x[i] + s.normalX[i] * d
                                        val y = s.y[i] + s.normalY[i] * d
                                        if (i == 0) ring.moveTo(x, y) else ring.lineTo(x, y)
                                    }
                                    ring.close()
                                    drawPath(
                                        ring,
                                        Ink.text.copy(alpha = 0.35f),
                                        style = Stroke(density, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * density, 5f * density))),
                                    )
                                }
                            }
                            if (showNormals) {
                                val arrows = 48
                                for (j in 0 until arrows) {
                                    val i = (j * n) / arrows
                                    val w = wobbleAt(topValues, top.segments, i.toFloat() / n - offset)
                                    val d = displacement(w, minPx, maxPx, swing)
                                    val base = Offset(s.x[i], s.y[i])
                                    val normal = Offset(s.normalX[i], s.normalY[i])
                                    line(base, base + normal * d, Ink.cool.copy(alpha = 0.7f), 1f)
                                    dot(base, 1.5f, Ink.cool)
                                }
                            }
                            if (showControl) {
                                topValues.forEachIndexed { k, v ->
                                    // Control point k sits where i / n - offset = k / count.
                                    val lap = k.toFloat() / top.segments + offset
                                    val i = ((lap - floor(lap)) * n).roundToInt() % n
                                    val d = displacement(v, minPx, maxPx, swing)
                                    dot(Offset(s.x[i] + s.normalX[i] * d, s.y[i] + s.normalY[i] * d), 5f, Ink.page, Color.White.copy(alpha = 0.9f))
                                }
                            }
                        }
                    }
                }
            }
            Well(3f, tag = "top layer's wobble, unrolled") {
                Canvas(Modifier.fillMaxSize()) {
                    frame.longValue
                    topMotion.values(clock.time(topIndex), topValues)
                    unrolledLap(topValues, measurer, labelStyle, offset = (top.rotation + clock.spin(topIndex)) / 360f)
                }
            }
        },
        right = {
            Eyebrow("Playground")
            H2("Everything at once")
            Note("The top layer uses the sliders directly. Aura layers are generated with the preset formulas from step 6. Turn on the overlays to see the machinery while it runs.")
            ControlsGrid(
                { IntSlider("segments", segments, 1..16) { segments = it } },
                { IntSlider("amplitude start", minDp, -40..20, ::dpLabel) { minDp = it } },
                { IntSlider("amplitude end", maxDp, -10..60, ::dpLabel) { maxDp = it } },
                { IntSlider("intensity", intensity, 0..150, ::x100) { intensity = it } },
                { IntSlider("tempo", tempo, 0..300, ::x100) { tempo = it } },
                { IntSlider("spin (°/s)", spin, -90..90) { spin = it } },
                { IntSlider("aura layers", auraCount, 0..6) { auraCount = it } },
                { IntSlider("aura reach", reach, 0..80, ::dpLabel) { reach = it } },
                { IntSlider("aura blur", blur, 0..24, ::dpLabel) { blur = it } },
            )
            ControlRow {
                Text("colour", style = Type.note())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palettes.forEachIndexed { i, p -> Swatch(p.body, i == palIndex) { palIndex = i } }
                }
            }
            ControlRow {
                CheckToggle("control points", showControl) { showControl = it }
                CheckToggle("normals", showNormals) { showNormals = it }
                CheckToggle("amplitude rings", showBand) { showBand = it }
            }
            val clipboard = LocalClipboard.current
            val scope = rememberCoroutineScope()
            ControlRow {
                PillButton("Copy as Kotlin") {
                    scope.launch {
                        copyStatus = try {
                            clipboard.setClipEntry(ClipEntry.withPlainText(code))
                            "Copied."
                        } catch (e: Throwable) {
                            "Couldn't reach the clipboard. Select the code below and copy it."
                        }
                    }
                }
                if (copyStatus.isNotEmpty()) Text(copyStatus, style = Type.note())
            }
            CodeBlock(code, highlight = false)
            Note("The blob here is the library's own `Blob`. The overlays and the strip come from a copy of its math with the same seeded Kotlin `Random`, the same outline sampling and the same per-layer clock, so they sit exactly on the real outline.")
        },
    )
}

private fun Float.trimmed(): String {
    val rounded = (this * 10f).roundToInt() / 10f
    return if (abs(rounded - rounded.roundToInt()) < 0.01f) rounded.roundToInt().toString() else rounded.fixed(1)
}

/** The Kotlin for what's on screen, in the page's format. */
private fun kotlinFor(layers: List<BlobLayer>, pal: BlobPalette, blur: Int, intensity: Int): String {
    val auraColor = "Color(0xFF${pal.auraHex})"
    val bodyColor = "Color(0xFF${pal.bodyHex})"
    val lines = layers.mapIndexed { i, l ->
        val min = l.amplitude.start.value.roundToInt()
        val max = l.amplitude.endInclusive.value
        if (i == layers.lastIndex) {
            """
            |    BlobLayer(
            |        color = $bodyColor,
            |        segments = ${l.segments},
            |        amplitude = ($min).dp..${max.roundToInt()}.dp,
            |        tempo = ${l.tempo.fixed(2)}f,
            |        spin = ${l.spin.roundToInt()}f,
            |    ),
            """.trimMargin()
        } else {
            val blurPart = if (blur > 0) ", blur = $blur.dp" else ""
            "    BlobLayer(color = $auraColor, alpha = ${l.alpha.fixed(2)}f, segments = ${l.segments}, " +
                "amplitude = (-4).dp..${max.trimmed()}.dp, tempo = ${l.tempo.fixed(2)}f, rotation = ${l.rotation.roundToInt()}f$blurPart),"
        }
    }
    return "Blob(\n  layers = listOf(\n${lines.joinToString("\n")}\n  ),\n  intensity = { ${(intensity / 100f).fixed(2)}f },\n  modifier = Modifier.size(240.dp),\n)"
}
