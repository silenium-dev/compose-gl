# compose-gl

Render OpenGL content onto a Compose Canvas.

## Supported platforms

- JVM + Linux
- JVM + Windows

## Usage

You can add the dependency to your project as follows:

```kotlin
repositories {
    maven("https://repo.silenium.dev/releases") {
        name = "silenium-dev-releases"
    }
}
dependencies {
    implementation("dev.silenium.compose.gl:compose-gl:0.7.4")
}
```

### Development Snapshots

Snapshots are available from [silenium-dev-snapshots](https://repo.silenium.dev/snapshots).
Versions don't follow semantic versioning, but are based on the commit hash: `<short-sha>-dev` (e.g. `c6d653e-dev`)

### Example

This is a simple example of how to use the library.
For a more complex example, see [src/test/kotlin/direct/Main.kt](src/test/kotlin/direct/Main.kt).

```kotlin
@Composable
fun App() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        // Button behind the GLCanvas -> Alpha works. Clicks will be passed through, as long as the GLCanvas is not clickable
        Button(onClick = {}) {
            Text("Click me!")
        }
        // Size needs to be specified, as the default size of a Compose Canvas is 0x0
        // Internally uses a Compose Canvas, so it can be used like any other Composable
        GLCanvas(modifier = Modifier.size(100.dp)) {
            // Translucent blue
            // Use LWJGL GL, other GL libraries may work but are not tested
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
