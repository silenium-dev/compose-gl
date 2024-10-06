# compose-gl

Render OpenGL content onto a Compose Canvas.

## Supported platforms

- JVM + Linux

## Dependencies

This library works with the default skiko and skia builds, 
but also supports a custom skiko+skia build using EGL and GLES instead of GLX and Desktop GL on Linux:

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
    implementation("dev.silenium.compose:compose-gl:0.4.1")
}
```

### Example

```kotlin
@Composable
fun App() {
    Box(contentAlignment = Alignment.TopStart) {
        // Button behind the GLSurfaceView -> Alpha works, clicks will be passed through, as long as the GLSurfaceView is not clickable
        Button(onClick = {}) {
            Text("Click me!")
        }
        // Size needs to be specified, as the default size is 0x0
        // Internally uses a Compose Canvas, so it can be used like any other Composable
        GLSurfaceView(
            modifier = Modifier.size(100.dp),
            presentMode = GLSurfaceView.PresentMode.MAILBOX, // Present mode is based on the Vulkan present modes
            swapChainSize = 2,
        ) {
            // Translucent grey
            // Use GLES or GL, depending on the skiko variant you are using
            GL30.glClearColor(0.5f, 0.5f, 0.5f, 0.5f)
            GL30.glClear(GL_COLOR_BUFFER_BIT)
            // Render with 30 FPS, this should be the time from start of frame n to frame n+1, the internal logic subtracts render time and other delays
            // Defaults to 60 FPS
            // Will be replaced with a better solution in the future
            redrawAfter((1000.0 / 30).milliseconds)
        }
    }
}

// awaitApplication{} is required for now. For some reason, the JVM gets stuck on shutdown, when using application{}.
fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
```
