# Module KBlobs

Morphing blobs for Compose Multiplatform on Android and iOS.

[Blob][dev.piotrprus.kblobs.Blob] draws a list of [BlobLayer][dev.piotrprus.kblobs.BlobLayer]s
around any `Shape`. Each layer moves its outline along the outward normal, with its own color,
alpha, blend mode, blur, number of segments, amplitude range, tempo, starting rotation and spin.

[BlobPresets][dev.piotrprus.kblobs.BlobPresets] has ready-made stacks to start from.

## Quick start

```kotlin
Blob(
    layers = BlobPresets.aura(body = Color(0xFFD3B26E)),
    modifier = Modifier.size(160.dp),
    intensity = { micLevel },
)
```

# Package dev.piotrprus.kblobs

The public API: the blob composable, its layer configuration and presets.
