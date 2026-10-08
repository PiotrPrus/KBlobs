package dev.piotrprus.kblobs.sample.web

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt

// ---------- page-wide state ----------

/** True when the global Pause button is pressed. */
internal val LocalPaused = compositionLocalOf { false }

/** True while the enclosing section is at least partly on screen. */
internal val LocalSectionVisible = compositionLocalOf { true }

/** True when the page is laid out in two columns. */
internal val LocalWide = compositionLocalOf { true }

/** Whether scenes in the current section should animate. */
@Composable
internal fun sectionRunning(): Boolean = !LocalPaused.current && LocalSectionVisible.current

/**
 * Calls [onFrame] with the frame's delta time while [running], the same way `Blob` does: the
 * first frame after a (re)start has dt 0 and a step is capped at 0.05 s. Returns a frame counter
 * to read inside draw lambdas, so each frame redraws without recomposing.
 */
@Composable
internal fun rememberFrameLoop(running: Boolean, onFrame: (dt: Float) -> Unit): State<Long> {
    val frame = remember { mutableLongStateOf(0L) }
    val latest by rememberUpdatedState(onFrame)
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                latest(dt)
                frame.longValue++
            }
        }
    }
    return frame
}

// ---------- layout ----------

/**
 * One step of the notebook: explanation on the left, live lab on the right; stacked when narrow.
 * Tracks its own visibility so off-screen sections stop animating.
 */
@Composable
internal fun StepSection(
    modifier: Modifier = Modifier,
    leftWeight: Float = 1f,
    rightWeight: Float = 1f,
    topRule: Boolean = true,
    left: @Composable ColumnScope.() -> Unit,
    right: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    val wide = LocalWide.current
    val ruled = if (topRule) {
        Modifier
            .drawBehind { drawLine(Ink.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(top = 40.dp)
    } else {
        Modifier
    }
    CompositionLocalProvider(LocalSectionVisible provides visible) {
        Box(
            modifier
                .fillMaxWidth()
                .onGloballyPositioned { c ->
                    val b = c.boundsInWindow()
                    visible = b.width > 0f && b.height > 0f
                }
                .then(ruled),
        ) {
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                    Column(Modifier.weight(leftWeight), verticalArrangement = Arrangement.spacedBy(18.dp), content = left)
                    Column(Modifier.weight(rightWeight), verticalArrangement = Arrangement.spacedBy(14.dp), content = right)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp), content = left)
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), content = right)
                }
            }
        }
    }
}

/** A rounded, dark canvas well. [tag] is the small caption in the top-left corner. */
@Composable
internal fun Well(
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    tag: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.panel),
    ) {
        content()
        if (tag != null) {
            Text(
                tag,
                style = Type.mono(11f).copy(color = Ink.muted, letterSpacing = 0.06.em),
                modifier = Modifier.padding(start = 12.dp, top = 8.dp),
            )
        }
    }
}

// ---------- text ----------

@Composable
internal fun Eyebrow(text: String) {
    Text(text.uppercase(), style = Type.eyebrow())
}

@Composable
internal fun H1(text: String) {
    Text(text, style = Type.h1(LocalWide.current))
}

@Composable
internal fun H2(text: String) {
    Text(text, style = Type.h2(LocalWide.current))
}

/**
 * Body text with a tiny markup: `code` becomes an inline code chip, *text* is italic.
 */
@Composable
internal fun Para(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = Type.body(),
) {
    val mono = LocalFonts.current.mono
    Text(markup(text, mono), style = style, modifier = modifier.widthIn(max = 640.dp))
}

@Composable
internal fun Note(text: String, modifier: Modifier = Modifier) {
    Para(text, modifier, Type.note())
}

internal fun markup(text: String, mono: androidx.compose.ui.text.font.FontFamily): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '`' || c == '*') {
                val end = text.indexOf(c, i + 1)
                if (end > i) {
                    val inner = text.substring(i + 1, end)
                    if (c == '`') {
                        withStyle(
                            SpanStyle(
                                fontFamily = mono,
                                fontSize = 0.88.em,
                                color = Ink.goldSoft,
                                background = Ink.gold.copy(alpha = 0.08f),
                            ),
                        ) { append(inner) }
                    } else {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(inner) }
                    }
                    i = end + 1
                    continue
                }
            }
            append(c)
            i++
        }
    }

/** A formula: monospaced, gold, with a gold rule on its left. Scrolls sideways when narrow. */
@Composable
internal fun Formula(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(Ink.gold, Offset(1.dp.toPx(), 0f), Offset(1.dp.toPx(), size.height), 2.dp.toPx()) }
            .horizontalScroll(rememberScrollState())
            .padding(start = 14.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Text(text, style = Type.mono(14f).copy(color = Ink.goldSoft), softWrap = false)
    }
}

private val keywordRegex = Regex("""\b(val|var|fun|private|class|for|in|until|if|else|return|init)\b""")
private val numberRegex = Regex("""\b(\d+(?:\.\d+)?f?)\b""")
private val functionRegex =
    Regex("""\b(wobbleAt|displacement|sin|floor|moveTo|lineTo|close|coerceIn|coerceAtMost|nextFloat)\b""")

/** The page's tiny Kotlin highlighter: comments, keywords, numbers and the library's functions. */
internal fun highlightKotlin(src: String): AnnotatedString = buildAnnotatedString {
    src.lines().forEachIndexed { lineIndex, line ->
        if (lineIndex > 0) append('\n')
        val ci = line.indexOf("//")
        val code = if (ci >= 0) line.substring(0, ci) else line
        val start = length
        append(code)
        listOf(
            keywordRegex to Ink.codeKeyword,
            numberRegex to Ink.codeNumber,
            functionRegex to Ink.goldSoft,
        ).forEach { (regex, color) ->
            regex.findAll(code).forEach { m ->
                addStyle(SpanStyle(color = color), start + m.range.first, start + m.range.last + 1)
            }
        }
        if (ci >= 0) withStyle(SpanStyle(color = Ink.codeComment)) { append(line.substring(ci)) }
    }
}

/** A code block: monospaced on the panel colour, selectable, scrolls sideways. */
@Composable
internal fun CodeBlock(code: String, highlight: Boolean = true, modifier: Modifier = Modifier) {
    val text = remember(code, highlight) { if (highlight) highlightKotlin(code) else AnnotatedString(code) }
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Ink.panel)
            .border(1.dp, Ink.line, RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        SelectionContainer {
            Text(text, style = Type.mono(13f), softWrap = false)
        }
    }
}

// ---------- controls ----------

/**
 * A labelled integer slider: label on the left, formatted value on the right, slider under them.
 * Works with mouse and touch.
 */
@Composable
internal fun IntSlider(
    label: String,
    value: Int,
    range: IntRange,
    format: (Int) -> String = { it.toString() },
    onChange: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = Type.note().copy(fontSize = 13.sp, lineHeight = 18.sp))
            Text(format(value), style = Type.mono(13f).copy(lineHeight = 18.sp))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { v -> v.roundToInt().coerceIn(range).let { if (it != value) onChange(it) } },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Ink.gold,
                activeTrackColor = Ink.gold,
                inactiveTrackColor = Ink.line,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().height(36.dp),
        )
    }
}

/** Lays out controls in as many 170dp+ columns as fit, like CSS `repeat(auto-fit, minmax(170px, 1fr))`. */
@Composable
internal fun ControlsGrid(vararg items: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 20.dp
        val columns = max(1, ((maxWidth + gap) / (170.dp + gap)).toInt()).coerceAtMost(items.size)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.toList().chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { item -> Box(Modifier.weight(1f)) { item() } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
internal fun CheckToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Checkbox) { onChange(!checked) }
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = Ink.gold,
                uncheckedColor = Ink.muted,
                checkmarkColor = Ink.page,
            ),
            modifier = Modifier.size(32.dp).padding(6.dp),
        )
        Text(label, style = Type.body().copy(fontSize = 14.sp, lineHeight = 20.sp, color = Ink.muted))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ControlRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

/** The page's pill button: gold, or outlined when [ghost]. */
@Composable
internal fun PillButton(text: String, ghost: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .clip(shape)
            .then(if (ghost) Modifier.background(Ink.page).border(1.dp, Ink.line, shape) else Modifier.background(Ink.gold))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Text(
            text,
            style = Type.body().copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (ghost) Ink.text else Ink.page,
            ),
        )
    }
}

/** A round colour swatch; ringed when selected. */
@Composable
internal fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) Ink.text else Color.Transparent, CircleShape)
            .padding(3.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(role = Role.RadioButton, onClick = onClick),
    )
}

/** A simple table: first column left-aligned, the others right-aligned, hairlines under rows. */
@Composable
internal fun MonoTable(header: List<String>, rows: List<List<String>>, columnWidth: Dp = 86.dp) {
    val style = Type.mono(12.5f)
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Column {
            (listOf(header) + rows).forEachIndexed { r, cells ->
                Row(
                    Modifier.drawBehind {
                        drawLine(Ink.line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    }.padding(vertical = 5.dp),
                ) {
                    cells.forEachIndexed { c, cell ->
                        Text(
                            cell,
                            style = if (r == 0) style.copy(color = Ink.muted) else style,
                            textAlign = if (c == 0) TextAlign.Start else TextAlign.End,
                            softWrap = false,
                            modifier = Modifier.width(if (c == 0) 96.dp else columnWidth).padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

internal fun dpLabel(v: Int): String = "${v}dp"
internal fun x100(v: Int): String = (v / 100f).fixed(2)

/** `toFixed` for the labels: Kotlin/Wasm has no String.format. */
internal fun Float.fixed(decimals: Int): String {
    var factor = 1
    repeat(decimals) { factor *= 10 }
    val scaled = kotlin.math.round(this * factor).toLong()
    val negative = scaled < 0
    val abs = kotlin.math.abs(scaled)
    val whole = abs / factor
    val frac = (abs % factor).toString().padStart(decimals, '0')
    val sign = if (negative) "-" else ""
    return if (decimals == 0) "$sign$whole" else "$sign$whole.$frac"
}
