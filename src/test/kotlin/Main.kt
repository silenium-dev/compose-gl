import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import kotlinx.coroutines.delay
import org.lwjgl.opengles.GLES30.*
import kotlin.time.Duration.Companion.milliseconds

@Composable
@Preview
fun ApplicationScope.App() {
    MaterialTheme {
        Box(contentAlignment = Alignment.TopStart, modifier = Modifier.fillMaxSize().background(Color.Black)) {
            Text("Hello, World!", color = Color.White, style = MaterialTheme.typography.h1)
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
                modifier = Modifier.aspectRatio(1f).fillMaxSize(),
            ) {
                glClearColor(color.red, color.green, color.blue, color.alpha)
//                glClearColor(1.0f, 0.2f, 0.6f, 0.5f)
                glClear(GL_COLOR_BUFFER_BIT)
                val wait = (1000.0 / 60).milliseconds
                redrawAfter(wait)
//                println("Delta time: $deltaTime")
//                println("Wait: $wait")
//                println("FPS: ${1_000_000.0 / deltaTime.inWholeMicroseconds}")
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
