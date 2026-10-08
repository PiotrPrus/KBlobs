package dev.piotrprus.kblobs.sample.web

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// Drawing helpers for the teaching canvases. They draw a circle directly (angle 0 at the top,
// clockwise) instead of sampling a Shape, which is what the library does for CircleShape too.
// Lengths passed in "dp" are multiplied by the density here.

private const val HALF_PI = (PI / 2).toFloat()

internal class EdgeOptions(
    val minDp: Float,
    val maxDp: Float,
    val intensity: Float = 1f,
    val rotation: Float = 0f,
    val spin: Float = 0f,
    val smooth: Boolean = true,
) {
    val offset: Float get() = (rotation + spin) / 360f
}

/** The moved edge of one layer on a circle, exactly like the library's buildPath(). */
internal fun DrawScope.circleEdge(c: Offset, r: Float, values: FloatArray, o: EdgeOptions, samples: Int? = null): Path {
    val count = values.size
    val n = samples ?: sampleCount(count)
    val path = Path()
    val minPx = o.minDp * density
    val maxPx = o.maxDp * density
    val offset = o.offset
    for (i in 0 until n) {
        val pos = i.toFloat() / n
        val a = TWO_PI * pos - HALF_PI
        val w = wobbleAt(values, count, pos - offset, o.smooth)
        val d = displacement(w, minPx, maxPx, o.intensity)
        val x = c.x + cos(a) * (r + d)
        val y = c.y + sin(a) * (r + d)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** Where control point [k] sits: at lap position k / count + offset, pushed by its own value. */
internal fun DrawScope.controlPoint(c: Offset, r: Float, k: Int, count: Int, value: Float, o: EdgeOptions): Pair<Offset, Float> {
    val a = TWO_PI * (k.toFloat() / count + o.offset) - HALF_PI
    val d = displacement(value, o.minDp * density, o.maxDp * density, o.intensity)
    return Offset(c.x + cos(a) * (r + d), c.y + sin(a) * (r + d)) to a
}

internal fun DrawScope.ring(c: Offset, r: Float, color: Color, dash: FloatArray? = null) {
    drawCircle(
        color = color,
        radius = max(0f, r),
        center = c,
        style = Stroke(
            width = density,
            pathEffect = dash?.let { PathEffect.dashPathEffect(FloatArray(it.size) { i -> it[i] * density }) },
        ),
    )
}

internal fun DrawScope.dot(p: Offset, radiusDp: Float, fill: Color?, stroke: Color? = null) {
    if (fill != null) drawCircle(fill, radiusDp * density, p)
    if (stroke != null) drawCircle(stroke, radiusDp * density, p, style = Stroke(2f * density))
}

internal fun DrawScope.line(from: Offset, to: Offset, color: Color, widthDp: Float) {
    drawLine(color, from, to, widthDp * density, cap = StrokeCap.Butt)
}

/** A lit-sphere fill for the body, like the page's radial gradient. */
internal fun bodyBrush(c: Offset, r: Float, pal: BlobPalette): Brush = Brush.radialGradient(
    0f to pal.light,
    0.55f to pal.body,
    1f to pal.dark,
    center = Offset(c.x - r * 0.35f, c.y - r * 0.4f),
    radius = r * 1.55f,
)

/** Text at [topLeft], in the canvas' mono caption style. */
internal fun DrawScope.caption(measurer: TextMeasurer, text: String, topLeft: Offset, style: TextStyle) {
    drawText(measurer, text, topLeft, style)
}

/**
 * The unrolled lap: x is position 0..1, y is the wobble -1..1. Control points are dots; the curve
 * is the same wobbleAt the circle uses, so the right edge continues into the left one.
 */
internal fun DrawScope.unrolledLap(
    values: FloatArray,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
    smooth: Boolean = true,
    offset: Float = 0f,
) {
    val w = size.width
    val h = size.height
    val padX = 18f * density
    val top = 30f * density
    val bottom = h - 18f * density
    fun yOf(v: Float) = top + (1f - (v + 1f) / 2f) * (bottom - top)
    fun xOf(p: Float) = padX + p * (w - 2 * padX)
    gridLines(padX, w - padX, ::yOf, measurer, labelStyle)
    val path = Path()
    val steps = 240
    for (i in 0..steps) {
        val p = i.toFloat() / steps
        val v = wobbleAt(values, values.size, p - offset, smooth)
        if (i == 0) path.moveTo(xOf(p), yOf(v)) else path.lineTo(xOf(p), yOf(v))
    }
    drawPath(path, Ink.gold, style = Stroke(2f * density))
    values.forEachIndexed { k, v ->
        var p = k.toFloat() / values.size + offset
        p -= floor(p)
        dot(Offset(xOf(p), yOf(v)), 4.5f, Ink.goldSoft)
        if (p < 1e-6f) dot(Offset(xOf(1f), yOf(v)), 4.5f, Ink.goldSoft)
    }
}

/** Hairlines at +1, 0 and −1 with their labels at the left edge. */
internal fun DrawScope.gridLines(
    x0: Float,
    x1: Float,
    yOf: (Float) -> Float,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
) {
    listOf(1f to "+1", 0f to " 0", -1f to "−1").forEach { (v, label) ->
        drawLine(Ink.line, Offset(x0, yOf(v)), Offset(x1, yOf(v)), density)
        val layout = measurer.measure(label, labelStyle)
        drawText(layout, topLeft = Offset(2f * density, yOf(v) - layout.size.height / 2f))
    }
}

/** min(width, height) of the canvas. */
internal val DrawScope.minSide: Float get() = min(size.width, size.height)
