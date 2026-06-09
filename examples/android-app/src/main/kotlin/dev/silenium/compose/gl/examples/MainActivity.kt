package dev.silenium.compose.gl.examples

import android.opengl.GLES32
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.silenium.compose.gl.canvas.GLCanvas
import dev.silenium.compose.gl.canvas.rememberGLCanvasState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.skia.BackendRenderTarget
import org.jetbrains.skia.Color
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.FramebufferFormat
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import org.jetbrains.skia.SurfaceColorFormat
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skia.TextLine
import org.slf4j.LoggerFactory
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val font = Font(
            FontMgr.default.matchFamily("sans-serif").matchStyle(FontStyle.NORMAL),
            64f
        )

        setContent {
            val state = rememberGLCanvasState()
            LaunchedEffect(Unit) {
                while (isActive) {
                    delay(1.seconds)
                    state.requestUpdate()
                }
            }

            var glSurface: Surface? by remember { mutableStateOf(null) }
            var glContext: DirectContext? by remember { mutableStateOf(null) }
            var glRenderTarget: BackendRenderTarget? by remember { mutableStateOf(null) }
            GLCanvas(state, Modifier.fillMaxSize(), onResize = { old, new ->
                log.info("Resized from {} to {}", old, new)
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
                    fbFormat = FramebufferFormat.GR_GL_RGBA8,
                )
                glSurface = Surface.makeFromBackendRenderTarget(
                    context = glContext!!, rt = glRenderTarget!!,
                    origin = SurfaceOrigin.BOTTOM_LEFT,
                    colorFormat = SurfaceColorFormat.RGBA_8888,
                    colorSpace = ColorSpace.sRGB,
                )
                check(glSurface != null)
            }, onDispose = {
                log.info("Disposed")
            }) {
                log.info(
                    "Rendering, glGetIntegerv addr: {}",
                    getGlProcAddress("glGetIntegerv").toHexString(),
                )
                GLES32.glClearColor(
                    Random.nextFloat(),
                    Random.nextFloat(),
                    Random.nextFloat(),
                    1.0f,
                )
                GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT)
                glContext?.resetAll()
                glSurface?.canvas?.let {
                    it.save()
                    it.translate(50f, 200f)
                    it.drawTextLine(
                        TextLine.make("Hello from Skia", font),
                        0f,
                        0f,
                        Paint().apply {
                            color = Color.BLACK
                            isAntiAlias = true
                        })
                    it.restore()
                }
                glContext?.flush()
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MainActivity::class.java)
    }
}
