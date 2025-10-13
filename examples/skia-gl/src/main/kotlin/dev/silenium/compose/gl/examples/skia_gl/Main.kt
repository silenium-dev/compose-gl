package dev.silenium.compose.gl.examples.skia_gl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.PlatformLayersComposeScene
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.silenium.compose.gl.canvas.GLCanvas
import dev.silenium.compose.gl.canvas.Stats
import dev.silenium.compose.gl.canvas.rememberGLCanvasState
import dev.silenium.compose.gl.canvas.resetGLFeatures
import dev.silenium.compose.gl.findSkiaLayer
import dev.silenium.compose.gl.graphicsApi
import org.jetbrains.skia.*
import org.jetbrains.skiko.Version

@OptIn(InternalComposeUiApi::class)
fun main() = application {
    val glScene = PlatformLayersComposeScene()
    glScene.setContent {
        Box(modifier = Modifier.wrapContentSize().background(Color.Red)) {
            Box(modifier = Modifier.wrapContentSize().padding(50.dp).background(Color.Blue)) {
                Text(
                    "Hello from Skia on OpenGL",
                    style = MaterialTheme.typography.h2,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.5f)).padding(10.dp),
                )
            }
        }
    }

    var glSurface: Surface? by mutableStateOf(null)
    var glContext: DirectContext? by mutableStateOf(null)
    var glRenderTarget: BackendRenderTarget? by mutableStateOf(null)

    val renderer = SampleRenderer()
    val state = rememberGLCanvasState()
    Window(onCloseRequest = ::exitApplication, title = "Test") {
        Box(Modifier.fillMaxSize()) {
            GLCanvas(
                state = state,
                modifier = Modifier.fillMaxSize(),
                onDispose = {
                    renderer.destroy()
                    println("Disposed")
                },
                onResize = { old, new ->
                    println("Resized from $old to $new, new fbo: ${fbo.id}")

                    if (glContext == null) {
                        glContext = DirectContext.makeGL()
                    }
                    glSurface?.close()
                    glRenderTarget?.close()
                    glRenderTarget = BackendRenderTarget.makeGL(
                        width = new.width,
                        height = new.height,
                        sampleCnt = 1,
                        stencilBits = 8,
                        fbId = fbo.id,
                        fbFormat = fbo.colorAttachment.internalFormat,
                    )
                    glSurface = Surface.makeFromBackendRenderTarget(
                        context = glContext!!, rt = glRenderTarget!!,
                        origin = SurfaceOrigin.TOP_LEFT,
                        colorFormat = SurfaceColorFormat.RGBA_8888,
                        colorSpace = ColorSpace.sRGB,
                    )
                },
            ) {
                glContext?.resetAll()
                glSurface?.canvas?.let {
                    it.save()
                    it.translate(50f, 200f)
                    glScene.render(it.asComposeCanvas(), System.nanoTime())
                    it.restore()
                }
                glSurface?.flushAndSubmit(true)
                glContext?.flush()
                glContext?.submit(true)
                resetGLFeatures()
                renderer.draw()
                state.requestUpdate()
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.surface,
                modifier = Modifier.padding(8.dp).wrapContentWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(8.dp).width(300.dp),
                ) {
                    Text("Skia Graphics API: ${window.findSkiaLayer()?.graphicsApi()}")
                    Text("Skia Version: ${Version.skia}")
                    Text("Skiko Version: ${Version.skiko}")
                    Button(onClick = { println("button pressed") }) {
                        Text("Button")
                    }

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
            }
        }
    }
}
