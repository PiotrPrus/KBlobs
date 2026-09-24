# KBlobs

Morphing blobs for Compose Multiplatform on Android and iOS.

A `Blob` is a stack of layers drawn around any `Shape`. Each layer pushes the outline in and out
along its normal and has its own settings:

| Setting | What it does |
|---|---|
| `color` / `brush` / `alpha` | Fill, or a gradient, and its opacity |
| `segments` | Number of control points around the outline. 4 gives a slow, calm wobble. 20 puts a new bump every 18° of a circle. |
| `amplitude` | Range the outline moves in, for example `(-3).dp..5.dp` to dip 3 dp in and bulge 5 dp out |
| `tempo` | Speed of the wobble |
| `rotation` | Starting angle of the wobble pattern, so layers can be offset from each other |
| `spin` | Degrees per second the pattern travels around the outline |
| `blur` | Blur radius (Android 12+; iOS always) |
| `blendMode` | How the layer composites over the ones below |
| `style` | `Fill` or `Stroke` |
| `seed` | Seed for the control points' motion |

A layer with `amplitude = 0.dp..0.dp` is the plain shape: use one on top as a solid, still body.

## Setup

```kotlin
commonMain.dependencies {
    implementation("io.github.piotrprus:kblobs:0.1.0")
}
```

## Quick start

```kotlin
Blob(
    layers = BlobPresets.aura(body = Color(0xFFD3B26E)),
    modifier = Modifier.size(160.dp),
)
```

Custom layers, around any shape, following a live level:

```kotlin
Blob(
    layers = listOf(
        BlobLayer(color = Color(0xFF4DABF7), alpha = 0.3f, segments = 5, amplitude = (-4).dp..18.dp, blur = 8.dp),
        BlobLayer(color = Color(0xFFDA77F2), alpha = 0.4f, segments = 12, amplitude = (-3).dp..8.dp, rotation = 40f, spin = 15f),
        BlobLayer(color = Color.White, amplitude = 0.dp..0.dp),
    ),
    shape = RoundedCornerShape(32.dp),
    intensity = { micLevel },   // read each frame, no recomposition
    speed = { 1f + micLevel },  // changes stay smooth: time is integrated, not rescaled
    modifier = Modifier.size(160.dp),
)
```

The shape fills the composable's bounds. Layers that move outward draw outside them, like a
shadow, so leave room around the blob.

`intensity` scales each layer's swing around the middle of its amplitude range: 0 holds the layer
still at the midpoint, 1 uses the full range. `animate = false` stops the motion, for reduced
motion.

Any `Shape` works: `CircleShape`, `RoundedCornerShape`, a `GenericShape`, or a Material 3
`MaterialShapes.*.toShape()`. Only the first contour of the outline is used.

## Sample

`samples/` has an Android and an iOS app. Add layers with **+**, open a layer with its chevron,
and change its color, alpha, segments, amplitude, tempo, rotation, spin, blur, blend mode and
style live.

```
./gradlew :samples:androidApp:installDebug
open samples/iosApp/iosApp.xcodeproj
```

## License

Apache 2.0
