import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.compose.gl.surface.GLSurfaceView
import dev.silenium.compose.gl.surface.Stats
import dev.silenium.compose.gl.surface.rememberGLSurfaceState
import kotlinx.coroutines.delay
import org.lwjgl.opengles.GLES30.*
import kotlin.time.Duration.Companion.milliseconds

@Composable
@Preview
fun ApplicationScope.App() {
    MaterialTheme {
        Box(contentAlignment = Alignment.TopStart, modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val state = rememberGLSurfaceState()
            var targetHue by remember { mutableStateOf(0f) }
            val color by animateColorAsState(
                Color.hsl(targetHue, 1f, 0.5f, 0.1f),
                animationSpec = tween(durationMillis = 200, easing = LinearEasing)
            )
            LaunchedEffect(Unit) {
                while (true) {
                    targetHue = (targetHue + 10f) % 360f
                    delay(200)
                }
            }
            GLSurfaceView(
                state = state,
                modifier = Modifier.aspectRatio(1f).fillMaxSize(),
                presentMode = GLSurfaceView.PresentMode.MAILBOX,
            ) {
                glClearColor(color.red, color.green, color.blue, color.alpha)
                glClear(GL_COLOR_BUFFER_BIT)
                val wait = (1000.0 / 60).milliseconds
                redrawAfter(wait)
            }
            Column(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                val display by state.displayStatistics.collectAsState()
                Text("Display datapoints: ${display.frameTimes.values.size}")
                Text("Display frame time: ${display.frameTimes.median.inWholeMicroseconds / 1000.0} ms")
                Text("Display frame time (99th): ${display.frameTimes.percentile(0.99).inWholeMicroseconds / 1000.0} ms")
                Text("Display FPS: ${display.fps.median}")
                Text("Display FPS (99th): ${display.fps.percentile(0.99, Stats.Percentile.LOWEST)}")

                val render by state.renderStatistics.collectAsState()
                Text("Render datapoints: ${render.frameTimes.values.size}")
                Text("Render frame time: ${render.frameTimes.median.inWholeMicroseconds / 1000.0} ms")
                Text("Render frame time (99th): ${render.frameTimes.percentile(0.99).inWholeMicroseconds / 1000.0} ms")
                Text("Render FPS: ${render.fps.median} ms")
                Text("Render FPS (99th): ${render.fps.percentile(0.99, Stats.Percentile.LOWEST)}")
            }
            Button(
                onClick = ::exitApplication,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
            ) {
                Text("Exit application")
            }
        }
    }
}

suspend fun main() = awaitApplication {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
