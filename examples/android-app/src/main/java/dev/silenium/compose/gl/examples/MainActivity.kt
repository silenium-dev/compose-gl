package dev.silenium.compose.gl.examples

import android.opengl.GLES32
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import dev.silenium.compose.gl.canvas.GLCanvas
import dev.silenium.compose.gl.canvas.rememberGLCanvasState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state = rememberGLCanvasState()
            LaunchedEffect(Unit) {
                while (isActive) {
                    delay(1.seconds)
                    state.requestUpdate()
                }
            }
            GLCanvas(state, Modifier.fillMaxSize(), onResize = { old, new ->
                log.info("Resized from {} to {}", old, new)
            }, onDispose = {
                log.info("Disposed")
            }) {
                log.info("Rendering")
                GLES32.glClearColor(
                    Random.nextFloat(),
                    Random.nextFloat(),
                    Random.nextFloat(),
                    1.0f,
                )
                GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MainActivity::class.java)
    }
}
