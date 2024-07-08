package dev.silenium.compose.gl.surface

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.LocalWindow
import dev.silenium.compose.gl.directContext
import dev.silenium.compose.gl.fbo.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import org.jetbrains.skia.*
import org.lwjgl.opengles.GLES32.GL_RGBA8
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds


@Composable
fun GLSurfaceView(
    modifier: Modifier = Modifier,
    paint: Paint = Paint(),
    presentMode: GLSurfaceView.PresentMode = GLSurfaceView.PresentMode.FIFO,
    swapChainSize: Int = 10,
    drawBlock: suspend GLDrawScope.() -> Unit,
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
        val job = surfaceView.launch()
        onDispose {
            job.cancel()
        }
    }
}

class GLSurfaceView(
    private val parentContext: EGLContext,
    private val drawBlock: suspend GLDrawScope.() -> Unit,
    private val invalidate: () -> Unit = {},
    private val paint: Paint = Paint(),
    private val presentMode: PresentMode = PresentMode.MAILBOX,
    private val swapChainSize: Int = 10,
) {
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
    private val updateRequest = Channel<Unit>(Channel.CONFLATED)
    private val executor = Executors.newSingleThreadExecutor {
        Thread(it, "GLSurfaceView-${index.getAndIncrement()}")
    }

    fun launch() = CoroutineScope(executor.asCoroutineDispatcher()).launch { run() }.also {
        it.invokeOnCompletion { executor.shutdown() }
    }

    fun resize(size: IntSize) {
        if (size == fboPool?.size) return
        this.size = size
        fboPool?.size = size
        updateRequest.trySend(Unit)
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

        fboPool = FBOPool(renderContext!!, parentContext, size, presentMode.impl, swapChainSize)
            .apply(FBOPool::initialize)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun run() = coroutineScope {
        while (size == IntSize.Zero && isActive) delay(10.milliseconds)
        if (!isActive) return@coroutineScope
        initEGL()
        var lastFrame: Long? = null
        while (isActive) {
            val now = System.nanoTime()
            val deltaTime = lastFrame?.let { now - it } ?: 0
            val waitTime = fboPool!!.render(deltaTime.nanoseconds, drawBlock) ?: continue
            invalidate()
            val renderTime = (System.nanoTime() - now).nanoseconds
            lastFrame = now
            try {
                select<Unit> {
                    updateRequest.onReceive { }
                    onTimeout(waitTime - renderTime) { }
                }
            } catch (e: CancellationException) {
                break
            }
        }
        fboPool?.destroy()
        fboPool = null
        directContext?.close()
        directContext = null
        renderContext?.destroy()
        renderContext = null
    }

    companion object {
        private val index = AtomicInteger(0)
    }
}
