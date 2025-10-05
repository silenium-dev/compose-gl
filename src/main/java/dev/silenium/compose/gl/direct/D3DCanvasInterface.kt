package dev.silenium.compose.gl.direct

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize
import dev.silenium.compose.gl.fbo.FBO
import dev.silenium.compose.gl.interop.D3DInterop
import dev.silenium.compose.gl.objects.Renderbuffer
import dev.silenium.compose.gl.objects.Texture
import dev.silenium.compose.gl.util.checkGLError
import org.jetbrains.skia.*
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.EXTMemoryObject
import org.lwjgl.opengl.EXTMemoryObject.GL_OPTIMAL_TILING_EXT
import org.lwjgl.opengl.EXTMemoryObject.GL_TEXTURE_TILING_EXT
import org.lwjgl.opengl.EXTMemoryObjectWin32
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL30.GL_DEPTH24_STENCIL8
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.system.MemoryUtil
import org.slf4j.LoggerFactory
import java.awt.Window

class D3DCanvasInterface(private val window: Window) : CanvasInterface {
    private var d3dDirectContext: DirectContext? by mutableStateOf(null)
    var d3dTexture: Long? = null
    var sharedHandle: Long? = null
    var backendTexture: BackendTexture? = null
    var image: Image? = null
    var initialized by mutableStateOf(false)

    var glMemory: Int? = null
    var glfwWindow = 0L
    var fbo: FBO? = null
    var glCaps: GLCapabilities? = null

    override fun setup(directContext: DirectContext) {
        d3dDirectContext = directContext
    }

    private fun ensureInitialized() {
        if (!initialized) {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)

            glfwWindow = GLFW.glfwCreateWindow(128, 128, "", MemoryUtil.NULL, MemoryUtil.NULL)
            if (glfwWindow == MemoryUtil.NULL) {
                error("Failed to create GLFW window")
            }
            GLFW.glfwMakeContextCurrent(glfwWindow)
            glCaps = GL.createCapabilities()
            GLFW.glfwMakeContextCurrent(MemoryUtil.NULL)
            initialized = true
        }
    }

    override fun render(
        scope: DrawScope,
        block: GLDrawScope.() -> Unit
    ) {
        val d3dCtx = d3dDirectContext ?: return
        ensureInitialized()
        GLFW.glfwMakeContextCurrent(glfwWindow)
        ensureFBO(scope.size.toIntSize(), d3dCtx)
        GLDrawScope(fbo!!).block()
        glFlush()
        GLFW.glfwMakeContextCurrent(MemoryUtil.NULL)
    }

    override fun display(scope: DrawScope) {
        d3dDirectContext ?: return
        ensureInitialized()
        val img = image ?: return log.warn("No image")
        scope.drawContext.canvas.nativeCanvas.drawImage(img, 0f, 0f)
    }

    override fun dispose() {
        GLFW.glfwMakeContextCurrent(glfwWindow)
        fbo?.destroy()
        image?.close()
        glMemory?.let(EXTMemoryObject::glDeleteMemoryObjectsEXT)
        d3dTexture?.let(D3DInterop::destroyTexture)
        sharedHandle?.let(D3DInterop::closeSharedHandle)
        GLFW.glfwMakeContextCurrent(MemoryUtil.NULL)
        glfwWindow.let(GLFW::glfwDestroyWindow)
        println("Disposed")
    }

    private fun ensureFBO(size: IntSize, d3dContext: DirectContext) {
        if (fbo?.size != size) {
            glMemory?.let(EXTMemoryObject::glDeleteMemoryObjectsEXT)
            glMemory = null
            sharedHandle?.let(D3DInterop::closeSharedHandle)
            sharedHandle = null
            d3dTexture?.let(D3DInterop::destroyTexture)
            d3dTexture = null
            backendTexture?.close()
            backendTexture = null
            image?.close()
            image = null
            fbo?.destroy()
            fbo = null

            d3dTexture = D3DInterop.createTexture(window, size.width, size.height)
            sharedHandle = D3DInterop.exportSharedHandle(window, d3dTexture!!)

            val colorAttachment = Texture(glGenTextures(), size, GL_TEXTURE_2D, GL_RGBA8)
            colorAttachment.bind()
            glTexParameteri(colorAttachment.target, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            checkGLError("glTexParameteri")
            glTexParameteri(colorAttachment.target, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            checkGLError("glTexParameteri")
            glTexParameteri(colorAttachment.target, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            checkGLError("glTexParameteri")
            glTexParameteri(colorAttachment.target, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            checkGLError("glTexParameteri")
            glTexParameteri(colorAttachment.target, GL_TEXTURE_TILING_EXT, GL_OPTIMAL_TILING_EXT)
            checkGLError("glTexParameteri")
            colorAttachment.unbind()

            glMemory = EXTMemoryObject.glCreateMemoryObjectsEXT()
            checkGLError("glCreateMemoryObjectsEXT")
            EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT(
                glMemory!!, size.width * size.height * 4 * 2L,
                EXTMemoryObjectWin32.GL_HANDLE_TYPE_D3D12_RESOURCE_EXT, sharedHandle!!,
            )
            checkGLError("glImportMemoryWin32HandleEXT")

            EXTMemoryObject.glTextureStorageMem2DEXT(
                colorAttachment.id,
                1, GL_RGBA8,
                size.width, size.height,
                glMemory!!, 0
            )
            checkGLError("glTextureStorageMem2DEXT")

            val depthStencilAttachment = Renderbuffer.create(size, GL_DEPTH24_STENCIL8)
            fbo = FBO.create(colorAttachment, depthStencilAttachment)

            backendTexture = D3DInterop.makeBackendTexture(d3dTexture!!)
            image = Image.adoptTextureFrom(
                d3dContext, backendTexture!!,
                SurfaceOrigin.TOP_LEFT, ColorType.RGBA_8888,
            )
        }
    }

    companion object : CanvasInterfaceFactory<D3DCanvasInterface> {
        private val log = LoggerFactory.getLogger(D3DCanvasInterface::class.java)

        override fun create(window: Window) = D3DCanvasInterface(window)

        init {
            GLFW.glfwInitHint(GLFW.GLFW_COCOA_MENUBAR, GLFW.GLFW_FALSE)
            if (!GLFW.glfwInit()) {
                throw RuntimeException("Failed to initialize GLFW")
            }
        }
    }
}
