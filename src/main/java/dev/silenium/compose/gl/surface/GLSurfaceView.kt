package dev.silenium.compose.gl.surface

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.EGL
import dev.silenium.compose.gl.LocalWindow
import dev.silenium.compose.gl.directContext
import dev.silenium.compose.gl.fbo.*
import org.jetbrains.skia.*
import org.lwjgl.opengles.GLES32.*
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.nanoseconds


@Composable
fun GLSurfaceView(
    modifier: Modifier = Modifier,
    paint: Paint = Paint(),
    presentMode: GLSurfaceView.PresentMode = GLSurfaceView.PresentMode.FIFO,
    swapChainSize: Int = 10,
    drawBlock: GLDrawScope.() -> Unit,
) {
    var invalidations by remember { mutableStateOf(0) }
    val surfaceView = remember {
        val currentContext = EGLContext.fromCurrent() ?: error("No current EGL context")
        GLSurfaceView(
            parentContext = currentContext,
            invalidate = { invalidations++ },
            paint = paint,
            presentMode = presentMode,
            swapChainSize = swapChainSize,
            drawBlock = drawBlock,
        )
    }
    val window = LocalWindow.current
    Canvas(
        modifier = modifier.onSizeChanged(surfaceView::resize)
    ) {
        invalidations.let {
            window?.directContext()?.let { directContext ->
                surfaceView.display(drawContext.canvas.nativeCanvas, directContext)
            }
        }
    }
    DisposableEffect(surfaceView) {
        surfaceView.start()
        onDispose {
            surfaceView.interrupt()
        }
    }
}

class GLSurfaceView(
    private val parentContext: EGLContext,
    private val drawBlock: GLDrawScope.() -> Unit,
    private val invalidate: () -> Unit = {},
    private val paint: Paint = Paint(),
    private val presentMode: PresentMode = PresentMode.MAILBOX,
    private val swapChainSize: Int = 10,
) : Thread("GLSurfaceView") {
    enum class PresentMode(internal val impl: (Int, (IntSize) -> FBOPool.FBO) -> IFBOPresentMode) {
        /**
         * Renders the latest frame and discards all the previous frames.
         * Results in the lowest latency.
         */
        MAILBOX(::FBOMailbox),

        /**
         * Renders all the frames in the order they were produced.
         * Results in a higher latency, but smoother animations.
         * Limits the framerate to the display refresh rate.
         */
        FIFO(::FBOFifo),
    }

    private var directContext: DirectContext? = null
    private var renderContext: EGLContext? = null
    private var size: IntSize = IntSize.Zero
    private var fboPool: FBOPool? = null
    private val updateRequested = AtomicBoolean(false)

    fun resize(size: IntSize) {
        if (size == fboPool?.size) return
        this.size = size
        fboPool?.size = size
        updateRequested.set(true)
    }

    fun display(canvas: Canvas, displayContext: DirectContext) {
        fboPool?.display { displayImpl(canvas, displayContext) }
        invalidate()
    }

    private fun GLDisplayScope.displayImpl(
        canvas: Canvas,
        displayContext: DirectContext
    ) {
        val rt = BackendRenderTarget.makeGL(
            fbo.size.width,
            fbo.size.height,
            1,
            8,
            fbo.id,
            GL_RGBA8,
        )
        val surface = Surface.makeFromBackendRenderTarget(
            displayContext,
            rt,
            SurfaceOrigin.TOP_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB
        ) ?: error("Failed to create surface")
        surface.draw(canvas, 0, 0, paint)
        surface.close()
        rt.close()
        displayContext.resetGLAll()
    }

    private fun initEGL() {
        renderContext = EGLContext.createOffscreen(parentContext)
        renderContext!!.makeCurrent()
        directContext = DirectContext.makeGL()
        EGL.debugContext()

        glEnable(GL_DEBUG_OUTPUT)
        glDebugMessageCallback({ _, type, _, severity, _, message, _ ->
            println(
                String.format(
                    "GL CALLBACK: %s type = 0x%x, severity = 0x%x, message = %s\n",
                    if (type == GL_DEBUG_TYPE_ERROR) "** GL ERROR **" else "",
                    type,
                    severity,
                    MemoryUtil.memUTF8(message)
                )
            )
        }, MemoryUtil.NULL)

        fboPool = FBOPool(renderContext!!, parentContext, size, presentMode.impl, swapChainSize)
            .apply(FBOPool::initialize)
    }

    override fun run() {
        while (size == IntSize.Zero && !isInterrupted) onSpinWait()
        if (isInterrupted) return
        initEGL()
        var lastFrame: Long? = null
        while (!isInterrupted) {
            val now = System.nanoTime()
            val deltaTime = lastFrame?.let { now - it } ?: 0
            val waitTime = fboPool!!.render(deltaTime.nanoseconds, drawBlock) ?: continue
            invalidate()
            val renderTime = System.nanoTime() - now
            lastFrame = now
            val nextFrame = now + waitTime.inWholeNanoseconds - renderTime
            while (
                System.nanoTime() <= nextFrame &&
                !isInterrupted &&
                !updateRequested.compareAndSet(true, false)
            ) {
                onSpinWait()
            }
        }
        fboPool?.destroy()
        fboPool = null
        directContext?.close()
        directContext = null
        renderContext?.destroy()
        renderContext = null
        println("GLSurfaceView thread exited")
    }
}
