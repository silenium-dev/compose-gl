package dev.silenium.compose.gl.canvas

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize
import dev.silenium.compose.gl.fbo.FBO
import dev.silenium.compose.gl.objects.Renderbuffer
import dev.silenium.compose.gl.objects.Texture
import org.jetbrains.skia.BackendTexture
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Image
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL33.GL_DEPTH24_STENCIL8
import org.lwjgl.opengl.GL33.GL_NEAREST
import org.lwjgl.opengl.GL33.GL_RGBA8
import org.lwjgl.opengl.GL33.GL_TEXTURE_2D
import org.lwjgl.opengl.GL33.glFlush
import org.lwjgl.opengl.GLCapabilities
import org.slf4j.LoggerFactory
import java.awt.Window

class GLCanvasDriver : CanvasDriver {
    var fbo: FBO? = null
    var image: Image? = null
    var initialized = false
    lateinit var glCapabilities: GLCapabilities
    var directContext: DirectContext? by mutableStateOf(null)
    var size: IntSize = IntSize.Zero

    override fun setup(directContext: DirectContext) {
        this.directContext = directContext
    }

    private fun ensureInitialized() {
        if (!initialized) {
            glCapabilities = GL.createCapabilities()
            initialized = true
        }
    }

    override fun dispose(userDisposeHandler: () -> Unit) {
        userDisposeHandler()
        image?.close()
        fbo?.destroySkikoCompatible()
    }

    override fun render(
        scope: DrawScope,
        userResizeHandler: FBOScope.(old: IntSize?, new: IntSize) -> Unit,
        block: FBOScope.() -> Unit
    ) {
        val ctx = directContext ?: return
        ensureInitialized()

        val oldSize = fbo?.size
        val newSize = scope.size.toIntSize()
        ensureFBO(newSize, ctx)
        if (oldSize != newSize) {
            FBOScopeImpl(fbo!!).userResizeHandler(oldSize, newSize)
        }

        FBOScopeImpl(fbo!!).block()
        glFlush()
        ctx.resetGLAll()
    }

    override fun display(scope: DrawScope) {
        directContext ?: return
        val img = image ?: return log.warn("No image")
        ensureInitialized()
        scope.drawContext.canvas.skiaCanvas.drawImage(img, 0f, 0f)
    }

    private fun ensureFBO(size: IntSize, ctx: DirectContext) {
        if (size != this.size) {
            image?.close()
            fbo?.destroySkikoCompatible()
            val fbo = createFBO(size).also { this.fbo = it }
            this.size = size

            val texture = BackendTexture.makeGL(
                width = fbo.size.width,
                height = fbo.size.height,
                isMipmapped = false,
                textureId = fbo.colorAttachment.id,
                textureTarget = fbo.colorAttachment.target,
                textureFormat = fbo.colorAttachment.internalFormat,
            )
            image = Image.adoptTextureFrom(
                context = ctx,
                backendTexture = texture,
                origin = SurfaceOrigin.TOP_LEFT,
                colorType = ColorType.RGBA_8888,
            )
        }
    }

    private fun createFBO(size: IntSize): FBO.Custom {
        val colorAttachment = Texture.create(
            target = GL_TEXTURE_2D, size = size, internalFormat = GL_RGBA8,
            wrapS = GL_CLAMP_TO_EDGE, wrapT = GL_CLAMP_TO_EDGE,
            minFilter = GL_NEAREST, magFilter = GL_NEAREST,
        )
        val depthStencil = Renderbuffer.create(size, GL_DEPTH24_STENCIL8)
        return FBO.create(colorAttachment, depthStencil)
    }

    override fun getGlProcAddress(name: String): Long =
        GL.getFunctionProvider()?.getFunctionAddress(name) ?: 0L

    companion object : CanvasDriverFactory<GLCanvasDriver> {
        private val log = LoggerFactory.getLogger(GLCanvasDriver::class.java)
        override fun create(window: Window) = GLCanvasDriver()
    }
}
