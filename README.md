# compose-gl

Render OpenGL content onto a Compose Canvas.

## Supported platforms

- JVM + Linux

## Dependencies

This library uses a custom skiko+skia build which uses EGL and GLES instead of GLX and Desktop GL on Linux:

- Skiko: https://github.com/silenium-dev/skiko
- Skia: https://github.com/silenium-dev/skia-pack

## Usage

You can add the dependency to your project as follows:

```kotlin
repositories {
    maven("https://reposilite.silenium.dev/releases") {
        name = "silenium-releases"
    }
}
dependencies {
    implementation("dev.silenium.compose:compose-gl:0.1.0")
}
```

### Example

```kotlin
@Composable
fun App() {
    Box(contentAlignment = Alignment.TopStart) {
        // Button behind the GLSurfaceView -> Alpha works, clicks will be passed through
        Button(onClick = {}) {
            Text("Click me!")
        }
        // Size needs to be specified, as the  default size is 0x0
        // Internally uses a Compose Canvas, so it can be used like any other Composable
        GLSurfaceView(modifier = Modifier.size(100.dp)) {
            // Translucent grey
            GLES30.glClearColor(0.5f, 0.5f, 0.5f, 0.5f)
            GLES30.glClear(GL_COLOR_BUFFER_BIT)
            // Render with 30 FPS, this should be the time from start of frame n to frame n+1, the internal logic subtracts render time and other delays
            // Defaults to 60 FPS
            // Will be replaced with a better solution in the future
            redrawAfter((1000.0 / 30).milliseconds)
        }
    }
}

// awaitApplication{} is required for now. For some reason, the JVM gets stuck on shutdown, when using application{}.
suspend fun main() = awaitApplication {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
```
