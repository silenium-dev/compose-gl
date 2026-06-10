# compose-gl

Render OpenGL content into a Composable.

## Supported platforms

- JVM + Linux
- JVM + Windows
- Android

## Usage

You can add the dependency to your project as follows:

```kotlin
repositories {
    maven("https://nexus.silenium.dev/repository/maven-releases/") {
        name = "silenium-dev-releases"
    }
}
dependencies {
    implementation("dev.silenium.compose.gl:compose-gl:0.7.4")
}
```

### Development Snapshots

Snapshots are available from [silenium-dev-snapshots](https://nexus.silenium.dev/repository/maven-snapshots/).

### Example

This is a simple example of how to use the library.
For more complex examples, see [examples](./examples).

```kotlin
@Composable
fun App() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        // Button behind the GLCanvas -> Alpha works. Clicks will be passed through, as long as the GLCanvas is not clickable
        Button(onClick = {}) {
            Text("Click me!")
        }
        // Size needs to be specified, as the default size of a GLCanvas is 0x0
        GLCanvas(modifier = Modifier.size(100.dp)) {
            // Translucent blue
            // Compatible libraries:
            // - Desktop: LWJGL GL
            // - Android: integrated GLESxx
            GL30.glClearColor(0f, 0f, 0.5f, 0.5f)
            GL30.glClear(GL30.GL_COLOR_BUFFER_BIT)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
```
