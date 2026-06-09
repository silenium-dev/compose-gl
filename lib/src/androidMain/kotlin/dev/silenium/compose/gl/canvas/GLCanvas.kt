package dev.silenium.compose.gl.canvas

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import dev.silenium.compose.gl.GLProvider.glViewport
import dev.silenium.compose.gl.fbo.FBO
import org.slf4j.LoggerFactory
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

private class GLCanvasRenderer(
    val state: GLCanvasState,
    val render: GLDrawScope.() -> Unit,
    val onResize: FBOScope.(old: IntSize?, new: IntSize) -> Unit,
    val onDispose: () -> Unit,
) : GLSurfaceView.Renderer {
    private var size: IntSize? = null
    private var fbo: FBO? = null

    override fun onSurfaceCreated(
        gl: GL10,
        config: EGLConfig,
    ) {
    }

    override fun onSurfaceChanged(
        gl: GL10,
        width: Int,
        height: Int,
    ) {
        val oldSize = size
        val size = IntSize(width, height).also { this.size = it }
        val fbo = FBO.Indestructible(0, size).also { this.fbo = it }
        val scope = FBOScopeImpl(fbo)
        scope.onResize(oldSize, size)
        glViewport(0, 0, size.width, size.height)
    }

    override fun onDrawFrame(gl: GL10) {
        val t1 = System.nanoTime()
        val delta = state.lastFrame?.let { (it - t1).nanoseconds } ?: Duration.ZERO

        val scope = GLDrawScopeImpl(FBOScopeImpl(fbo ?: return log.error("No FBO")), delta)
        scope.drawGL {
            scope.render()
        }

        val t2 = System.nanoTime()
        val d1 = (t2 - t1).nanoseconds
        state.onRender(t2, d1)
        state.onDisplay(t2, d1)
    }

    companion object {
        private val log = LoggerFactory.getLogger(GLCanvasRenderer::class.java)
    }
}

@Composable
actual fun GLCanvas(
    state: GLCanvasState,
    modifier: Modifier,
    onDispose: () -> Unit,
    onResize: FBOScope.(old: IntSize?, new: IntSize) -> Unit,
    block: GLDrawScope.() -> Unit,
) {
    val renderer = remember { GLCanvasRenderer(state, block, onResize, onDispose) }
    AndroidView(
        factory = {
            GLSurfaceView(it).apply {
                setEGLContextClientVersion(3)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                preserveEGLContextOnPause = true
            }
        },
        modifier = modifier,
    ) { view ->
        state.invalidations.let {
            view.requestRender()
        }
    }
}
