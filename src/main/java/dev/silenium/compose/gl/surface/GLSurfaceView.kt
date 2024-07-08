package dev.silenium.compose.gl.surface

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.*
import org.jetbrains.skia.*
import org.lwjgl.opengles.GLES32.*
import org.lwjgl.system.MemoryUtil
import kotlin.time.Duration.Companion.nanoseconds

@Composable
fun GLSurfaceView(modifier: Modifier = Modifier, drawBlock: GLDrawScope.() -> Unit) {
    var invalidations by remember { mutableStateOf(0) }
    val surfaceView = remember {
        val currentContext = EGLContext.fromCurrent() ?: error("No current EGL context")
        GLSurfaceView(
            parentContext = currentContext,
            drawBlock = drawBlock,
            invalidate = { invalidations++ }
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
    LaunchedEffect(surfaceView) {
        surfaceView.start()
    }
}

class GLSurfaceView(
    private val parentContext: EGLContext,
    private val drawBlock: GLDrawScope.() -> Unit,
    private val invalidate: () -> Unit = {},
) : Thread("GLSurfaceView") {
    private var directContext: DirectContext? = null
    private var renderContext: EGLContext? = null
    private var size: IntSize = IntSize.Zero
    private var fboPool: FBOPool? = null

    fun resize(size: IntSize) {
        if (size == fboPool?.size) return
        this.size = size
        fboPool?.size = size
    }

    fun display(canvas: Canvas, directContext: DirectContext) {
        fboPool?.display { displayImpl(canvas, directContext) }
        if (fboPool == null) invalidate()
    }

    private fun GLDisplayScope.displayImpl(
        canvas: Canvas,
        directContext: DirectContext
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
            directContext,
            rt,
            SurfaceOrigin.TOP_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB
        ) ?: error("Failed to create surface")
        surface.draw(canvas, 0, 0, null)
//        val bitmap = Bitmap()
//        bitmap.allocPixels(ImageInfo(fbo.size.width, fbo.size.height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE))
//        surface.readPixels(bitmap, 0, 0)
//        snapshot(bitmap, Path("main.png"))
//        bitmap.close()
        surface.close()
        rt.close()
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

        fboPool = FBOPool(renderContext!!, parentContext, size).apply {
            initialize()
        }
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
//            println("Last frame: $lastFrame, Current frame: $now, Delta time: $deltaTime")
            lastFrame = now
//            println("Render time: $renderTime")
            val nextFrame = now + waitTime.inWholeNanoseconds - renderTime
//            println("Current frame: $now, Next frame: $nextFrame, waitTime: $waitTime")
            while (System.nanoTime() < nextFrame && !isInterrupted);
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
