package dev.silenium.compose.gl.direct

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.toIntSize
import dev.silenium.compose.gl.LocalWindow
import dev.silenium.compose.gl.directContext
import dev.silenium.compose.gl.fbo.FBO
import dev.silenium.compose.gl.fbo.FBODrawScope
import dev.silenium.compose.gl.fbo.draw
import dev.silenium.compose.gl.objects.Renderbuffer
import dev.silenium.compose.gl.objects.Texture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.BackendTexture
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Image
import org.jetbrains.skia.SurfaceOrigin
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL33
import org.lwjgl.opengl.GLCapabilities
import org.slf4j.LoggerFactory

private class GLDirectCanvasState {
    var fbo: FBO? = null
    var image: Image? = null
    var texture: BackendTexture? = null
    var initialized = false
    lateinit var glCapabilities: GLCapabilities
    var directContext: DirectContext? by mutableStateOf(null)
    var size: Size = Size.Zero

    fun draw(scope: DrawScope, block: FBODrawScope.() -> Unit) {
        val ctx = directContext ?: return log.warn("No direct context")
        ctx.submit(syncCpu = true)
        ensureInitialized()
        ensureFBO(scope.size, ctx)
        fbo!!.draw(block)
        ctx.resetGLAll()
    }

    fun display(scope: DrawScope) {
        directContext ?: return log.warn("No direct context")
        val img = image ?: return log.warn("No image")
        ensureInitialized()
        scope.drawContext.canvas.nativeCanvas.drawImage(img, 0f, 0f)
    }

    private fun ensureInitialized() {
        if (!initialized) {
            glCapabilities = GL.createCapabilities()
            initialized = true
        }
    }

    private fun ensureFBO(size: Size, ctx: DirectContext) {
        if (size != this.size) {
            image?.close()
            fbo?.id?.let(GL30::glDeleteFramebuffers)
            fbo?.depthStencilAttachment?.destroy()
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
                origin = SurfaceOrigin.BOTTOM_LEFT,
                colorType = ColorType.RGBA_8888,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GLDirectCanvasState::class.java)
    }
}

@Composable
fun GLDirectCanvas(modifier: Modifier = Modifier, block: FBODrawScope.() -> Unit) {
    val state = remember { GLDirectCanvasState() }
    val window = LocalWindow.current ?: throw IllegalStateException("No window")
    LaunchedEffect(window) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                window.directContext()?.let {
                    state.directContext = it
                    return@withContext
                }
            }
        }
    }
    Canvas(modifier) {
        state.directContext ?: return@Canvas
        state.draw(this, block)
        state.display(this)
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
