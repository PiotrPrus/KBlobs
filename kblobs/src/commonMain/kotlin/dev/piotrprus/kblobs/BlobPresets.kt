package dev.piotrprus.kblobs

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Ready-made layer stacks to start from. Each returns a plain list, so copy and tweak freely. */
public object BlobPresets {

    /**
     * A still, solid body wrapped in translucent layers that morph out of phase: an aura.
     *
     * @param body Color of the solid shape on top.
     * @param aura Color of the moving layers; each one gets a lower alpha the further out it is.
     * @param layers Number of moving layers.
     * @param reach How far the outermost layer can bulge beyond the body.
     */
    public fun aura(
        body: Color,
        aura: Color = body,
        layers: Int = 3,
        reach: Dp = 24.dp,
    ): List<BlobLayer> {
        require(layers >= 0) { "layers must not be negative, was $layers" }
        val moving = (0 until layers).map { i ->
            // Outermost first, so the denser inner layers draw over it.
            val depth = if (layers == 1) 1f else 1f - i.toFloat() / layers
            BlobLayer(
                color = aura,
                alpha = 0.12f + 0.14f * (1f - depth),
                segments = 5 + i * 2,
                amplitude = (-4).dp..reach * depth,
                tempo = 0.7f + 0.35f * i,
                rotation = i * 47f,
            )
        }
        return moving + BlobLayer(color = body, amplitude = 0.dp..0.dp)
    }
}
