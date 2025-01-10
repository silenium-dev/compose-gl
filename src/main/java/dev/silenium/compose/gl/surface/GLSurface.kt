package dev.silenium.compose.gl.surface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.LocalWindow
import dev.silenium.compose.gl.context.GLContext
import dev.silenium.compose.gl.context.GLContextProvider
import dev.silenium.compose.gl.context.GLContextProviderFactory
import dev.silenium.compose.gl.directContext
import dev.silenium.compose.gl.fbo.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30.GL_RGBA8
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.toJavaDuration

/**
 * Override the size of the FBO.
 * @param width The width of the FBO.
 * @param height The height of the FBO.
 * @param transformOrigin The transform origin for scaling the FBO to the size of the GLSurfaceView (default: [TransformOrigin.Center]).
 */
data class FBOSizeOverride(
    val width: Int,
    val height: Int,
    val transformOrigin: TransformOrigin = TransformOrigin.Center,
) {
    val size get() = IntSize(width, height)
}

/**
 * A composable that displays OpenGL content.
 * @param state The state of the [GLSurface].
 * @param modifier The modifier to apply to the [GLSurface].
 * @param paint The paint to draw the contents on the compose scene.
 * @param glContextProvider The provider of the OpenGL context (default: [GLContextProviderFactory.detected]).
 * @param presentMode The present mode of the GLSurfaceView (default: [GLSurface.PresentMode.FIFO]).
 * @param swapChainSize The size of the swap chain (default: 10).
 * @param fboSizeOverride The size override of the FBO (default: null).
 * @param cleanup The cleanup block to run when the [GLSurface] is destroyed (default: {}).
 * @param draw The draw block to render the OpenGL content.
 * @see [GLDrawScope]
 */
@Composable
fun GLSurfaceView(
    state: GLSurfaceState = rememberGLSurfaceState(),
    modifier: Modifier = Modifier,
    paint: Paint = Paint(),
    glContextProvider: GLContextProvider<*> = GLContextProviderFactory.detected,
    presentMode: GLSurface.PresentMode = GLSurface.PresentMode.FIFO,
    swapChainSize: Int = 10,
    fboSizeOverride: FBOSizeOverride? = null,
    cleanup: () -> Unit = {},
    draw: GLDrawScope.() -> Unit,
) {
    val surfaceView = rememberGLSurface(
        state = state,
        glContextProvider = glContextProvider,
        presentMode = presentMode,
        swapChainSize = swapChainSize,
        fboSizeOverride = fboSizeOverride,
        cleanup = cleanup,
        draw = draw,
    )
    GLSurfaceView(surfaceView, modifier, paint)
}

/**
 * A composable that remembers a [GLSurface].
 * @param state The state of the [GLSurface].
 * @param glContextProvider The provider of the OpenGL context (default: [GLContextProviderFactory.detected]).
 * @param presentMode The present mode of the GLSurfaceView (default: [GLSurface.PresentMode.FIFO]).
 * @param swapChainSize The size of the swap chain (default: 10).
 * @param fboSizeOverride The size override of the FBO (default: null).
 * @param cleanup The cleanup block to run when the [GLSurface] is destroyed (default: {}).
 * @param draw The draw block to render the OpenGL content.
 */
@Composable
fun rememberGLSurface(
    state: GLSurfaceState = rememberGLSurfaceState(),
    glContextProvider: GLContextProvider<*> = GLContextProviderFactory.detected,
    presentMode: GLSurface.PresentMode = GLSurface.PresentMode.FIFO,
    swapChainSize: Int = 10,
    fboSizeOverride: FBOSizeOverride? = null,
    cleanup: () -> Unit = {},
    draw: GLDrawScope.() -> Unit,
): GLSurface {
    val surfaceView = remember(state, glContextProvider, presentMode, swapChainSize, cleanup, draw) {
        val currentContext = glContextProvider.fromCurrent() ?: error("No current EGL context")
        GLSurface(
            state = state,
            parentContext = currentContext,
            presentMode = presentMode,
            swapChainSize = swapChainSize,
            cleanupBlock = cleanup,
            drawBlock = draw,
            fboSizeOverride = fboSizeOverride,
        )
    }
    DisposableEffect(surfaceView) {
        surfaceView.launch()
        onDispose {
            surfaceView.interrupt()
        }
    }
    LaunchedEffect(fboSizeOverride) {
        surfaceView.fboSizeOverride = fboSizeOverride
    }

    return surfaceView
}

/**
 * A composable that displays OpenGL content.
 * @param surface The [GLSurface] to display.
 * @param modifier The modifier to apply to the [GLSurface].
 * @param paint The paint to draw the contents on the compose scene.
 */
@Composable
fun GLSurfaceView(
    surface: GLSurface,
    modifier: Modifier = Modifier,
    paint: Paint = Paint(),
) {
    val window = LocalWindow.current
    var directContext by remember { mutableStateOf<DirectContext?>(null) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(window) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                window?.directContext()?.let {
                    directContext = it
                    return@withContext
                }
            }
        }
    }
    BoxWithConstraints(modifier) {
        Canvas(
            modifier = Modifier
                .onSizeChanged {
                    componentSize = it
                    if (surface.fboSizeOverride == null) {
                        surface.resize(it)
                    }
                }.let {
                    val override = surface.fboSizeOverride
                    if (override != null) {
                        it.matchParentSize()
                            .drawWithContent {
                                val xScale = size.width / override.width
                                val yScale = size.height / override.height
                                val scale = minOf(xScale, yScale)
                                translate(
                                    (size.width - override.width * scale) * override.transformOrigin.pivotFractionX,
                                    (size.height - override.height * scale) * override.transformOrigin.pivotFractionY,
                                ) {
                                    scale(scale, Offset.Zero) {
                                        this@drawWithContent.drawContent()
                                    }
                                }
                            }
                    } else {
                        it.matchParentSize()
                    }
                }
        ) {
            surface.invalidations.let {
                directContext?.let { directContext ->
                    surface.display(drawContext.canvas.nativeCanvas, directContext, paint)
                }
            }
        }
    }
    LaunchedEffect(surface.fboSizeOverride) {
        val fboSizeOverride = surface.fboSizeOverride
        if (fboSizeOverride != null) {
            surface.resize(fboSizeOverride.size)
        } else {
            surface.resize(componentSize)
        }
    }
}

class GLSurface internal constructor(
    private val state: GLSurfaceState,
    private val parentContext: GLContext<*>,
    private val drawBlock: GLDrawScope.() -> Unit,
    private val cleanupBlock: () -> Unit = {},
    private val presentMode: PresentMode = PresentMode.MAILBOX,
    private val swapChainSize: Int = 10,
    fboSizeOverride: FBOSizeOverride? = null,
) : Thread("GLSurfaceView-${index.getAndIncrement()}") {
    enum class PresentMode(internal val impl: (Int, (IntSize) -> FBO) -> FBOSwapChain) {
        /**
         * Renders the latest frame and discards all the previous frames.
         * Results in the lowest latency.
         */
        MAILBOX(::FBOMailboxSwapChain),

        /**
         * Renders all the frames in the order they were produced.
         * Results in a higher latency, but smoother animations.
         * Limits the framerate to the display refresh rate.
         */
        FIFO(::FBOFifoSwapChain),
    }

    private var directContext: DirectContext? = null
    private var renderContext: GLContext<*>? = null
    private var size: IntSize = IntSize.Zero
    private var fboPool: FBOPool? = null
    internal var invalidations by mutableStateOf(0L)
    internal var fboSizeOverride: FBOSizeOverride? by mutableStateOf(fboSizeOverride)

    internal fun launch() {
        GL.createCapabilities()
        start()
//        return CoroutineScope(executor.asCoroutineDispatcher()).launch {
//            run()
//        }.also {
//            it.invokeOnCompletion { executor.shutdown() }
//        }
    }

    internal fun resize(size: IntSize) {
        if (size == fboPool?.size) return
        this.size = size
        fboPool?.size = size
        state.requestUpdate()
    }

    internal fun display(canvas: Canvas, displayContext: DirectContext, paint: Paint) {
        val t1 = System.nanoTime()
        fboPool?.display { displayImpl(canvas, displayContext, paint) }
        invalidations = t1
        val t2 = System.nanoTime()
        state.onDisplay(t2, (t2 - t1).nanoseconds)
    }

    private fun GLDisplayScope.displayImpl(
        canvas: Canvas,
        displayContext: DirectContext,
        paint: Paint,
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

    private fun initialize() {
        renderContext = parentContext.deriveOffscreenContext()
        renderContext!!.makeCurrent()
        directContext = DirectContext.makeGL()

        fboPool = FBOPool(renderContext!!, parentContext, size, presentMode.impl, swapChainSize)
            .apply(FBOPool::initialize)
    }

    override fun run() {
        while (size == IntSize.Zero && !isInterrupted) sleep(10)
        if (isInterrupted) return
        initialize()
        var lastFrame: Long? = null
        while (!isInterrupted) {
            val renderStart = System.nanoTime()
            val deltaTime = lastFrame?.let { renderStart - it } ?: 0
            val renderResult = fboPool!!.render(deltaTime.nanoseconds, drawBlock)
            val e = renderResult.exceptionOrNull()
            if (e is NoRenderFBOAvailable) {
                try {
                    sleep(1)
                } catch (e: InterruptedException) {
                    break
                }
                continue
            } else if (e is CancellationException || e is InterruptedException) {
                break
            } else if (e != null) {
                logger.error("Failed to render frame", e)
                break
            }
            val waitTime = renderResult.getOrNull()
            val renderEnd = System.nanoTime()
            invalidations = renderEnd
            state.onRender(renderEnd, (renderEnd - renderStart).nanoseconds)
            lastFrame = renderStart
            try {
                if (waitTime != null) {
                    val toWait = (waitTime - (System.nanoTime() - renderStart).nanoseconds).toJavaDuration()
                    if (!toWait.isZero) {
                        state.updateRequested.poll(toWait.toNanos(), TimeUnit.NANOSECONDS)
                    }
                } else {
                    state.updateRequested.take()
                }
            } catch (e: InterruptedException) {
                break
            }
        }
        logger.debug("GLSurfaceView stopped")
        cleanupBlock()
        fboPool?.destroy()
        fboPool = null
        directContext?.close()
        directContext = null
        renderContext?.destroy()
        renderContext = null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GLSurface::class.java)
        private val index = AtomicLong(0L)
    }
}
