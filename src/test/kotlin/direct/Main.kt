package direct

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.scene.PlatformLayersComposeScene
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.silenium.compose.gl.direct.GLCanvas
import dev.silenium.compose.gl.graphicsApi
import direct_import.GLTextureDrawer
import org.jetbrains.skiko.Version

@OptIn(InternalComposeUiApi::class)
fun main() = application {
    val glScene = PlatformLayersComposeScene()
    glScene.setContent {
        Text("Hello from Skia on OpenGL", style = MaterialTheme.typography.h2)
    }

    val drawer = GLTextureDrawer()
    Window(onCloseRequest = ::exitApplication, title = "Test") {
        Box(Modifier.fillMaxSize()) {
            DisposableEffect(Unit) {
                onDispose {
                    drawer.destroy()
                }
            }
            GLCanvas(modifier = Modifier.fillMaxSize()) {
                drawer.render()
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.surface,
                modifier = Modifier.padding(8.dp).wrapContentWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Skia Graphics API: ${window.graphicsApi()}")
                    Text("Skia Version: ${Version.skia}")
                    Text("Skiko Version: ${Version.skiko}")
                    Button(onClick = { println("button pressed") }) {
                        Text("Button")
                    }
                }
            }
        }
    }
}
