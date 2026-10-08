package dev.piotrprus.kblobs.sample.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.piotrprus.kblobs.sample.web.resources.Res
import dev.piotrprus.kblobs.sample.web.resources.bricolage_grotesque_bold
import dev.piotrprus.kblobs.sample.web.resources.ibm_plex_sans_regular
import dev.piotrprus.kblobs.sample.web.resources.ibm_plex_sans_semibold
import dev.piotrprus.kblobs.sample.web.resources.jetbrains_mono_regular
import dev.piotrprus.kblobs.sample.web.resources.jetbrains_mono_semibold
import org.jetbrains.compose.resources.Font

/** The page palette: dark navy and gold. */
internal object Ink {
    val page = Color(0xFF0B1020)
    val panel = Color(0xFF121A2E)
    val line = Color(0xFF24304A)
    val text = Color(0xFFE8E4DA)
    val muted = Color(0xFF98A0B3)
    val gold = Color(0xFFD3B26E)
    val goldSoft = Color(0xFFF1E2BD)
    val cool = Color(0xFF8FA3BF)

    // Code highlighting.
    val codeComment = Color(0xFF6B7590)
    val codeKeyword = Color(0xFFC99A5B)
    val codeNumber = Color(0xFF9DB7D6)
}

/** Colours for the blob body (a lit sphere) and its aura. */
@Immutable
internal data class BlobPalette(
    val name: String,
    val body: Color,
    val light: Color,
    val dark: Color,
    val aura: Color,
    val bodyHex: String,
    val auraHex: String,
)

internal val Palettes = listOf(
    BlobPalette("Gold", Color(0xFFD3B26E), Color(0xFFF6E7C1), Color(0xFF9A7638), Color(0xFFD9BC7C), "D3B26E", "D9BC7C"),
    BlobPalette("Ivory", Color(0xFFE4DDD0), Color(0xFFFFFFFF), Color(0xFFA39C8F), Color(0xFFE9E2D3), "E4DDD0", "E9E2D3"),
    BlobPalette("Slate", Color(0xFF8FA3BF), Color(0xFFD5DEEB), Color(0xFF4F5F78), Color(0xFF9DB0CB), "8FA3BF", "9DB0CB"),
    BlobPalette("Clay", Color(0xFFC98B67), Color(0xFFF1D2BE), Color(0xFF85523A), Color(0xFFD39A78), "C98B67", "D39A78"),
)

@Immutable
internal class Fonts(
    val display: FontFamily,
    val body: FontFamily,
    val mono: FontFamily,
)

internal val LocalFonts = staticCompositionLocalOf {
    Fonts(FontFamily.Default, FontFamily.Default, FontFamily.Monospace)
}

@Composable
internal fun rememberFonts(): Fonts = Fonts(
    display = FontFamily(Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold)),
    body = FontFamily(
        Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    ),
    mono = FontFamily(
        Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold),
    ),
)

internal object Type {
    @Composable fun body(): TextStyle = TextStyle(
        fontFamily = LocalFonts.current.body,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        color = Ink.text,
    )

    @Composable fun lede(): TextStyle = body().copy(fontSize = 19.sp, lineHeight = 30.sp)

    @Composable fun note(): TextStyle = body().copy(fontSize = 14.sp, lineHeight = 22.sp, color = Ink.muted)

    @Composable fun h1(wide: Boolean): TextStyle = TextStyle(
        fontFamily = LocalFonts.current.display,
        fontWeight = FontWeight.Bold,
        fontSize = if (wide) 56.sp else 40.sp,
        lineHeight = if (wide) 59.sp else 43.sp,
        letterSpacing = (-0.01).em,
        color = Ink.text,
    )

    @Composable fun h2(wide: Boolean): TextStyle = h1(wide).copy(
        fontSize = if (wide) 33.sp else 27.sp,
        lineHeight = if (wide) 38.sp else 31.sp,
    )

    @Composable fun eyebrow(): TextStyle = TextStyle(
        fontFamily = LocalFonts.current.mono,
        fontSize = 12.sp,
        letterSpacing = 0.14.em,
        color = Ink.gold,
    )

    @Composable fun mono(size: Float = 13f): TextStyle = TextStyle(
        fontFamily = LocalFonts.current.mono,
        fontSize = size.sp,
        lineHeight = (size * 1.55f).sp,
        color = Ink.text,
    )
}
