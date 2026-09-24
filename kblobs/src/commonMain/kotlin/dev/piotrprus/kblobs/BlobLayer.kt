package dev.piotrprus.kblobs

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One morphing outline drawn by [Blob].
 *
 * The layer starts from the blob's shape and pushes every point of the outline along its normal.
 * [segments] control points sit at equal distances around the outline. Each one moves on its own
 * smooth, seeded curve inside [amplitude], and the outline between two control points follows a
 * smooth curve through them. A layer with an amplitude of `0.dp..0.dp` is the plain shape, which
 * is how you draw a solid, still body under or over the moving layers.
 *
 * @property color Fill color. Ignored when [brush] is set.
 * @property brush Optional brush, for example a gradient, used instead of [color].
 * @property alpha Opacity multiplier in `0..1`, applied on top of the color or brush.
 * @property segments Number of control points around the outline. 4 gives a slow, calm wobble;
 *   20 puts a new bump every 18° of a circle and makes the outline busy.
 * @property amplitude How far the outline can move from the shape, along the outward normal.
 *   Negative values move inward. `(-3).dp..5.dp` lets the outline dip 3 dp in and bulge 5 dp out.
 * @property tempo Speed multiplier for the wobble. 1 is the default pace, 0 freezes the layer.
 * @property rotation Starting angle of the wobble pattern in degrees. The pattern is shifted along
 *   the outline, so two layers with the same seed and a different rotation are offset from each
 *   other. On a circle this is the same as rotating the layer.
 * @property spin Degrees per second the wobble pattern travels around the outline. Negative spins
 *   the other way.
 * @property blur Blur radius. Needs Android 12 (API 31) or later; older Android draws the layer
 *   sharp. iOS always blurs.
 * @property blendMode How the layer is composited with what is already drawn under it.
 * @property style [Fill] or a [androidx.compose.ui.graphics.drawscope.Stroke] for an outline only.
 * @property seed Seed of the control points' motion. Null derives a seed from the layer's position
 *   in the list, so layers move independently without any setup.
 */
@Immutable
public data class BlobLayer(
    val color: Color = Color.White,
    val brush: Brush? = null,
    val alpha: Float = 1f,
    val segments: Int = 6,
    val amplitude: ClosedRange<Dp> = (-4).dp..4.dp,
    val tempo: Float = 1f,
    val rotation: Float = 0f,
    val spin: Float = 0f,
    val blur: Dp = 0.dp,
    val blendMode: BlendMode = BlendMode.SrcOver,
    val style: DrawStyle = Fill,
    val seed: Int? = null,
) {
    init {
        require(segments >= 1) { "segments must be at least 1, was $segments" }
        require(alpha in 0f..1f) { "alpha must be in 0..1, was $alpha" }
        require(tempo >= 0f) { "tempo must not be negative, was $tempo" }
        require(blur >= 0.dp) { "blur must not be negative, was $blur" }
        require(amplitude.start <= amplitude.endInclusive) {
            "amplitude must not be empty, was $amplitude"
        }
    }
}
