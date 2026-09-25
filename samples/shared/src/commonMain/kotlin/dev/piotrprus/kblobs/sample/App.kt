package dev.piotrprus.kblobs.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.kblobs.Blob
import dev.piotrprus.kblobs.BlobLayer
import dev.piotrprus.kblobs.BlobPresets
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

private val Background = Color(0xFF0E0D12)

private val Palette = listOf(
    Color(0xFFD3B26E), Color(0xFFF1DDA9), Color(0xFFFF6B6B), Color(0xFFFFA94D),
    Color(0xFFFFE066), Color(0xFF69DB7C), Color(0xFF38D9A9), Color(0xFF4DABF7),
    Color(0xFF748FFC), Color(0xFFDA77F2), Color(0xFFF783AC), Color(0xFFFFFFFF),
)

private val BlendModes = listOf(
    "Normal" to BlendMode.SrcOver,
    "Plus" to BlendMode.Plus,
    "Screen" to BlendMode.Screen,
    "Multiply" to BlendMode.Multiply,
    "Overlay" to BlendMode.Overlay,
    "Lighten" to BlendMode.Lighten,
    "Difference" to BlendMode.Difference,
)

/** A layer in the editor. The id keeps each card's expanded state attached to its layer. */
private data class LayerEntry(
    val id: Int,
    val layer: BlobLayer,
    val expanded: Boolean = false,
)

private val StarShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val outer = size.minDimension / 2f
    val inner = outer * 0.55f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val a = -PI / 2 + i * PI / 5
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun sampleShapes(): List<Pair<String, Shape>> {
    val cookie = MaterialShapes.Cookie9Sided.toShape()
    return remember(cookie) {
        listOf(
            "Circle" to CircleShape,
            "Rounded" to RoundedCornerShape(28),
            "Cookie" to cookie,
            "Star" to StarShape,
        )
    }
}

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme(background = Background, surface = Color(0xFF1A1820))) {
        var nextId by remember { mutableIntStateOf(0) }
        val entries = remember {
            mutableStateListOf<LayerEntry>().apply {
                BlobPresets.aura(body = Palette[0], aura = Palette[1]).forEach {
                    add(LayerEntry(id = nextId++, layer = it))
                }
            }
        }
        val shapes = sampleShapes()
        var shapeIndex by remember { mutableIntStateOf(0) }
        var intensity by remember { mutableFloatStateOf(1f) }
        var speed by remember { mutableFloatStateOf(1f) }
        var showcase by remember { mutableStateOf(false) }
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        if (showcase) {
            Showcase(onClose = { showcase = false })
            return@MaterialTheme
        }

        Scaffold(
            containerColor = Background,
            floatingActionButton = {
                FloatingActionButton(onClick = {
                    val layer = BlobLayer(
                        color = Palette.random(),
                        alpha = 0.35f,
                        segments = Random.nextInt(4, 12),
                        amplitude = (-6).dp..14.dp,
                        rotation = Random.nextInt(0, 360).toFloat(),
                    )
                    // New layers open, the rest close, so the list stays short.
                    for (i in entries.indices) entries[i] = entries[i].copy(expanded = false)
                    entries += LayerEntry(id = nextId++, layer = layer, expanded = true)
                    // +1 for the global controls item above the cards.
                    scope.launch { listState.animateScrollToItem(entries.size) }
                }) {
                    Text("+", fontSize = 28.sp)
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Box(
                    Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Blob(
                        layers = entries.map { it.layer },
                        modifier = Modifier.size(170.dp),
                        shape = shapes[shapeIndex].second,
                        intensity = { intensity },
                        speed = { speed },
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        GlobalControls(
                            shapes = shapes.map { it.first },
                            shapeIndex = shapeIndex,
                            onShape = { shapeIndex = it },
                            intensity = intensity,
                            onIntensity = { intensity = it },
                            speed = speed,
                            onSpeed = { speed = it },
                            onShowcase = { showcase = true },
                        )
                    }
                    itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                        LayerCard(
                            index = index,
                            count = entries.size,
                            entry = entry,
                            onChange = { entries[index] = it },
                            onRemove = { entries.removeAt(index) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalControls(
    shapes: List<String>,
    shapeIndex: Int,
    onShape: (Int) -> Unit,
    intensity: Float,
    onIntensity: (Float) -> Unit,
    speed: Float,
    onSpeed: (Float) -> Unit,
    onShowcase: () -> Unit,
) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shapes.forEachIndexed { i, name ->
                FilterChip(selected = i == shapeIndex, onClick = { onShape(i) }, label = { Text(name) })
            }
        }
        LabeledSlider("Intensity", intensity, 0f..2f, format = { it.fmt(2) }, onChange = onIntensity)
        LabeledSlider("Speed", speed, 0f..4f, format = { "${it.fmt(2)}×" }, onChange = onSpeed)
        OutlinedButton(onClick = onShowcase, modifier = Modifier.fillMaxWidth()) {
            Text("Showcase: all four demos")
        }
    }
}

@Composable
private fun LayerCard(
    index: Int,
    count: Int,
    entry: LayerEntry,
    onChange: (LayerEntry) -> Unit,
    onRemove: () -> Unit,
) {
    val layer = entry.layer
    fun update(block: BlobLayer.() -> BlobLayer) = onChange(entry.copy(layer = layer.block()))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onChange(entry.copy(expanded = !entry.expanded)) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .alpha(if (layer.visible) 1f else 0.4f)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(layer.color.copy(alpha = layer.alpha.coerceAtLeast(0.15f)))
                    .border(1.dp, layer.color, CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).alpha(if (layer.visible) 1f else 0.4f)) {
                val position = when (index) {
                    0 -> " · bottom"
                    count - 1 -> " · top"
                    else -> ""
                }
                Text("Layer ${index + 1}$position", fontWeight = FontWeight.SemiBold)
                Text(
                    "${layer.segments} segments · ${layer.amplitude.start.value.roundToInt()}..${layer.amplitude.endInclusive.value.roundToInt()} dp · ${layer.tempo.fmt(1)}×",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = layer.visible, onCheckedChange = { update { copy(visible = it) } })
            Spacer(Modifier.width(8.dp))
            Chevron(expanded = entry.expanded)
        }

        AnimatedVisibility(entry.expanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                Text("Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Palette.forEach { color ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (color == layer.color) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape,
                                )
                                .clickable { update { copy(color = color) } },
                        )
                    }
                }
                LabeledSlider("Alpha", layer.alpha, 0f..1f, format = { it.fmt(2) }) {
                    update { copy(alpha = it) }
                }
                LabeledSlider(
                    label = "Segments",
                    value = layer.segments.toFloat(),
                    range = 1f..32f,
                    steps = 30,
                    format = { it.roundToInt().toString() },
                ) { update { copy(segments = it.roundToInt()) } }
                Text(
                    "Amplitude ${layer.amplitude.start.value.fmt(1)} .. ${layer.amplitude.endInclusive.value.fmt(1)} dp",
                    style = MaterialTheme.typography.labelMedium,
                )
                RangeSlider(
                    value = layer.amplitude.start.value..layer.amplitude.endInclusive.value,
                    onValueChange = { update { copy(amplitude = it.start.dp..it.endInclusive.dp) } },
                    valueRange = -40f..40f,
                )
                LabeledSlider("Tempo", layer.tempo, 0f..4f, format = { "${it.fmt(2)}×" }) {
                    update { copy(tempo = it) }
                }
                LabeledSlider("Rotation", layer.rotation, 0f..360f, format = { "${it.roundToInt()}°" }) {
                    update { copy(rotation = it) }
                }
                LabeledSlider("Spin", layer.spin, -180f..180f, format = { "${it.roundToInt()}°/s" }) {
                    update { copy(spin = it) }
                }
                LabeledSlider("Blur", layer.blur.value, 0f..32f, format = { "${it.roundToInt()} dp" }) {
                    update { copy(blur = it.dp) }
                }
                Text("Blend mode", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BlendModes.forEach { (name, mode) ->
                        FilterChip(
                            selected = layer.blendMode == mode,
                            onClick = { update { copy(blendMode = mode) } },
                            label = { Text(name) },
                        )
                    }
                }
                LabeledSlider(
                    label = "Thickness",
                    value = layer.strokeWidth.value,
                    range = 0f..24f,
                    format = { if (it < 0.05f) "Fill" else "${it.fmt(1)} dp stroke" },
                ) { update { copy(strokeWidth = if (it < 0.05f) 0.dp else it.dp) } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { update { copy(seed = Random.nextInt()) } }) { Text("Reshuffle") }
                    TextButton(onClick = onRemove) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun Chevron(expanded: Boolean) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(24.dp).rotate(rotation)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        drawLine(color, Offset(w * 0.25f, h * 0.4f), Offset(w * 0.5f, h * 0.62f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.62f), Offset(w * 0.75f, h * 0.4f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(format(value), style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

private fun Float.fmt(decimals: Int): String {
    var factor = 1
    repeat(decimals) { factor *= 10 }
    val rounded = (this * factor).roundToInt()
    val sign = if (rounded < 0) "-" else ""
    val abs = kotlin.math.abs(rounded)
    if (decimals == 0) return "$sign$abs"
    return "$sign${abs / factor}.${(abs % factor).toString().padStart(decimals, '0')}"
}
