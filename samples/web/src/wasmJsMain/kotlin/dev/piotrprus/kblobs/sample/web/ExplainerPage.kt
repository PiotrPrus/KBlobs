package dev.piotrprus.kblobs.sample.web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/** The whole explainer: one long page, two columns when wide, one when narrow. */
@Composable
internal fun ExplainerPage(startPaused: Boolean) {
    var paused by remember { mutableStateOf(startPaused) }
    val hand = remember { HandValues() }
    val fonts = rememberFonts()
    CompositionLocalProvider(LocalFonts provides fonts, LocalPaused provides paused) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Ink.page)) {
            val wide = maxWidth >= 880.dp
            val gutter = if (maxWidth < 600.dp) 16.dp else 20.dp
            CompositionLocalProvider(LocalWide provides wide) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = gutter)
                        .padding(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.widthIn(max = 1180.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(72.dp),
                    ) {
                        Hero()
                        StepOutline()
                        StepControlPoints(hand)
                        StepAmplitude(hand)
                        StepMotion()
                        StepRotation()
                        StepLayers()
                        Playground()
                        Footer()
                    }
                }
            }
            PillButton(
                if (paused) "Play" else "Pause",
                ghost = true,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) { paused = !paused }
        }
    }
}

@Composable
private fun Footer() {
    val uri = LocalUriHandler.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        listOf(
            "KBlobs on GitHub" to "https://github.com/PiotrPrus/KBlobs",
            "API docs" to "https://piotrprus.github.io/KBlobs/",
        ).forEach { (label, url) ->
            Text(
                label,
                style = Type.note().copy(color = Ink.gold, textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable(role = Role.Button) { uri.openUri(url) },
            )
        }
    }
}
