package dev.piotrprus.kblobs.sample

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.piotrprus.kblobs.Blob
import dev.piotrprus.kblobs.BlobLayer
import kotlinx.coroutines.delay

/** 1. One filled circle with few segments: the calmest the blob gets. */
private val Calm = listOf(
    BlobLayer(
        color = Color(0xFFD3B26E),
        segments = 4,
        amplitude = (-6).dp..6.dp,
        tempo = 0.6f,
    ),
)

/** 2. One outline with more segments and a slow spin, still calm. */
private val Outlined = listOf(
    BlobLayer(
        color = Color(0xFFFF6B6B),
        segments = 10,
        amplitude = (-5).dp..7.dp,
        tempo = 0.7f,
        spin = 14f,
        strokeWidth = 2.5.dp,
    ),
)

/**
 * 3. Four layers: a soft gradient fill under three outlines, each with its own color or gradient,
 * segments, rotation and spin. Screen blending brightens the outlines where they cross.
 */
private val Layered = listOf(
    BlobLayer(
        brush = Brush.linearGradient(listOf(Color(0xFFF783AC), Color(0xFFFFA94D))),
        alpha = 0.22f,
        segments = 5,
        amplitude = (-4).dp..14.dp,
        tempo = 0.6f,
        spin = 6f,
    ),
    BlobLayer(
        color = Color(0xFF4DABF7),
        segments = 6,
        amplitude = (-6).dp..12.dp,
        tempo = 0.9f,
        rotation = 60f,
        spin = -14f,
        strokeWidth = 3.dp,
        blendMode = BlendMode.Screen,
    ),
    BlobLayer(
        brush = Brush.sweepGradient(
            listOf(Color(0xFF38D9A9), Color(0xFF748FFC), Color(0xFFDA77F2), Color(0xFF38D9A9)),
        ),
        segments = 8,
        amplitude = (-8).dp..10.dp,
        tempo = 1.2f,
        rotation = 140f,
        spin = 18f,
        strokeWidth = 2.5.dp,
        blendMode = BlendMode.Screen,
    ),
    BlobLayer(
        color = Color(0xFFFFA94D),
        segments = 11,
        amplitude = (-5).dp..8.dp,
        tempo = 0.9f,
        rotation = 220f,
        spin = -22f,
        strokeWidth = 2.dp,
        blendMode = BlendMode.Screen,
    ),
)

/** 4. The same four layers, blurred into a glow. The fill gets more blur than the outlines. */
private val Blurred = Layered.mapIndexed { i, layer ->
    layer.copy(blur = if (i == 0) 14.dp else 5.dp)
}

private val Demos = listOf(
    "Few segments, filled" to Calm,
    "More segments, outlined" to Outlined,
    "Four layers, gradients" to Layered,
    "Four layers, blurred" to Blurred,
)

/**
 * All four demos on one screen, for recording. The close button fades out after a few seconds
 * so it stays out of the recording; tap anywhere to bring it back.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Showcase(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    // Bumped on every tap; the close button stays visible for a while after the last one.
    var touches by remember { mutableIntStateOf(0) }
    var showClose by remember { mutableStateOf(true) }
    LaunchedEffect(touches) {
        showClose = true
        delay(2_500)
        showClose = false
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0D12))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { touches++ }
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Demos.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { (label, layers) ->
                        DemoCell(label, layers, Modifier.weight(1f))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showClose,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun DemoCell(label: String, layers: List<BlobLayer>, modifier: Modifier) {
    Column(modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            // 60% of the cell leaves room for the outer layers, which draw outside the blob.
            Blob(layers = layers, modifier = Modifier.size(maxWidth * 0.6f))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}
