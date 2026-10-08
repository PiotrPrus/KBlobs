package dev.piotrprus.kblobs.sample.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Respect the system setting: start paused when the visitor asked for reduced motion.
    val reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    document.getElementById("loading")?.remove()
    ComposeViewport(document.body!!) {
        ExplainerPage(startPaused = reducedMotion)
    }
}
