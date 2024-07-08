# compose-gl

Render OpenGL content onto a Compose Canvas.

## Supported platforms

- JVM + Linux

## Usage

### Dependencies

This library uses a custom skiko+skia build which uses EGL and GLES instead of GLX and Desktop GL on Linux:
- Skiko: https://github.com/silenium-dev/skiko
- Skia: https://github.com/silenium-dev/skia-pack

Both are based on the official builds by JetBrains.

You need to build and publish the Skiko library locally before using this library:
Ensure your `JAVA_HOME` is set to a JDK 17 installation.
```shell
git clone https://github.com/silenium-dev/skiko.git
cd skiko/skiko
./gradlew publishToMavenLocal
```

### Building

The library is not published to any repository yet, so you need to clone the repository and publish it locally:

```shell
git clone https://github.com/silenium-dev/compose-gl.git
cd compose-gl
./gradlew publishToMavenLocal
```

Then you can add the dependency to your project:

```kotlin
repositories {
    mavenLocal()
}
dependencies {
    implementation("dev.silenium.compose:compose-gl:0.0.0-SNAPSHOT")
    
    implementation("org.jetbrains.skiko:skiko-awt") {
        version {
            strictly("0.0.0-SNAPSHOT")
        }
        isChanging = true
    }

    implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64") {
        version {
            strictly("0.0.0-SNAPSHOT")
        }
        isChanging = true
    }
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
            glClearColor(0.5f, 0.5f, 0.5f, 0.5f)
            glClear(GL_COLOR_BUFFER_BIT)
            // Render with 30 FPS, this should be the time from start of frame n to frame n+1, the internal logic subtracts render time and other delays
            // Defaults to 60 FPS
            // Will be replaced with a better solution in the future
            redrawAfter((1000.0 / 30).milliseconds)
        }
    }
}
```
