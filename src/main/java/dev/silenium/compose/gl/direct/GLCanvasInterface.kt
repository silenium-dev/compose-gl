package dev.silenium.compose.gl.direct

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.toIntSize
import dev.silenium.compose.gl.fbo.FBO
import dev.silenium.compose.gl.objects.Renderbuffer
import dev.silenium.compose.gl.objects.Texture
import org.jetbrains.skia.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glFlush
import org.lwjgl.opengl.GL33
import org.lwjgl.opengl.GLCapabilities
import org.slf4j.LoggerFactory
import java.awt.Window

class GLCanvasInterface : CanvasInterface {
    var fbo: FBO? = null
    var image: Image? = null
    var texture: BackendTexture? = null
    var initialized = false
    lateinit var glCapabilities: GLCapabilities
    var directContext: DirectContext? by mutableStateOf(null)
    var size: Size = Size.Zero

    override fun setup(directContext: DirectContext) {
        this.directContext = directContext
    }

    private fun ensureInitialized() {
        if (!initialized) {
            glCapabilities = GL.createCapabilities()
            initialized = true
        }
    }

    override fun dispose() {
        texture?.close()
        fbo?.destroy()
    }

    override fun render(
        scope: DrawScope,
        block: GLDrawScope.() -> Unit
    ) {
        val ctx = directContext ?: return
        ctx.submit(syncCpu = true)
        ensureInitialized()
        log.trace("Ensuring FBO")
        ensureFBO(scope.size, ctx)
        log.trace("Drawing to FBO")
        GLDrawScope(fbo!!).block()
        log.trace("Flushing")
        glFlush()
        ctx.resetGLAll()
        log.trace("Rendering done")
    }

    override fun display(scope: DrawScope) {
        directContext ?: return
        val img = image ?: return log.warn("No image")
        ensureInitialized()
        scope.drawContext.canvas.nativeCanvas.drawImage(img, 0f, 0f)
    }

    private fun ensureFBO(size: Size, ctx: DirectContext) {
        if (size != this.size) {
            texture?.close()
            fbo?.destroy()
            val fbo = createFBO(size).also { this.fbo = it }
            this.size = size

            val texture = BackendTexture.makeGL(
                width = fbo.size.width,
                height = fbo.size.height,
                isMipmapped = false,
                textureId = fbo.colorAttachment.id,
                textureTarget = fbo.colorAttachment.target,
                textureFormat = fbo.colorAttachment.internalFormat,
            ).also { this.texture = it }
            image = Image.adoptTextureFrom(
                context = ctx,
                backendTexture = texture,
                origin = SurfaceOrigin.TOP_LEFT,
                colorType = ColorType.RGBA_8888,
            )
        }
    }

    private fun createFBO(size: Size): FBO {
        val colorAttachment = Texture.create(
            target = GL11.GL_TEXTURE_2D, size = size.toIntSize(), internalFormat = GL11.GL_RGBA8,
            wrapS = GL33.GL_CLAMP_TO_EDGE, wrapT = GL33.GL_CLAMP_TO_EDGE,
            minFilter = GL33.GL_NEAREST, magFilter = GL33.GL_NEAREST,
        )
        val depthStencil = Renderbuffer.create(size.toIntSize(), GL33.GL_DEPTH24_STENCIL8)
        return FBO.create(colorAttachment, depthStencil)
    }

    companion object : CanvasInterfaceFactory<GLCanvasInterface> {
        private val log = LoggerFactory.getLogger(GLCanvasInterface::class.java)
        override fun create(window: Window) = GLCanvasInterface()
    }
}
