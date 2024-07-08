package dev.silenium.compose.gl

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.silenium.compose.gl.surface.GLSurfaceView
import org.lwjgl.opengles.GLES30.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

@Composable
@Preview
fun App() {
    MaterialTheme {
        Box(contentAlignment = Alignment.TopStart) {
            Button(onClick = {}) {
                Text("Click me!")
            }
            GLSurfaceView(modifier = Modifier.aspectRatio(1f).fillMaxSize()) {
//                glClearColor(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1.0f)
                glClearColor(1.0f, 0.2f, 0.6f, 0.5f)
                glClear(GL_COLOR_BUFFER_BIT)
                val wait = (1000.0 / 240).milliseconds
                redrawAfter(wait)
//                println("Delta time: $deltaTime")
//                println("Wait: $wait")
//                println("FPS: ${1_000_000.0 / deltaTime.inWholeMicroseconds}")
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
