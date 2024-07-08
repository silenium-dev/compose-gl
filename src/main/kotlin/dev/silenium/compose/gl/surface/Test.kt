package dev.silenium.compose.gl.surface

import org.lwjgl.egl.EGL15
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.glfwGetWindowContentScale
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL46
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import kotlin.system.exitProcess

fun main() {
    var width = 640
    var height = 480

    GLFW.glfwInit()
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
    GLFW.glfwWindowHint(GLFW.GLFW_EGL_CONTEXT_API, GLFW.GLFW_OPENGL_API)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_EGL_CONTEXT_API)
    val windowHandle = GLFW.glfwCreateWindow(width, height, "Compose LWJGL Demo", MemoryUtil.NULL, MemoryUtil.NULL)
    GLFW.glfwMakeContextCurrent(windowHandle)
    GLFW.glfwSwapInterval(1)

    GL.createCapabilities()

    println("EGL context: " + EGL15.eglGetCurrentContext())

    GL46.glEnable(GL46.GL_DEBUG_OUTPUT)
    GL46.glDebugMessageCallback({ _, type, _, severity, _, message, _ ->
        println(
            String.format(
                "GL CALLBACK: %s type = 0x%x, severity = 0x%x, message = %s\n",
                if (type == GL46.GL_DEBUG_TYPE_ERROR) "** GL ERROR **" else "",
                type,
                severity,
                MemoryUtil.memUTF8(message)
            )
        )
    }, MemoryUtil.NULL)

    var colorAttachment = GL46.glGenTextures()
    GL46.glBindTexture(GL46.GL_TEXTURE_2D, colorAttachment)
    GL46.glTexImage2D(
        GL46.GL_TEXTURE_2D,
        0,
        GL46.GL_RGBA,
        width,
        height,
        0,
        GL46.GL_RGBA,
        GL46.GL_UNSIGNED_BYTE,
        MemoryUtil.NULL
    )

    var rbo = GL46.glGenRenderbuffers()
    GL46.glBindRenderbuffer(GL46.GL_RENDERBUFFER, rbo)
    GL46.glRenderbufferStorage(GL46.GL_RENDERBUFFER, GL46.GL_DEPTH24_STENCIL8, width, height)

    var fbo = GL46.glGenFramebuffers()
    GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, fbo)
    GL46.glFramebufferTexture2D(GL46.GL_FRAMEBUFFER, GL46.GL_COLOR_ATTACHMENT0, GL46.GL_TEXTURE_2D, colorAttachment, 0)
    GL46.glFramebufferRenderbuffer(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_STENCIL_ATTACHMENT, GL46.GL_RENDERBUFFER, rbo)
    GL46.glDrawBuffer(GL46.GL_COLOR_ATTACHMENT0)
    if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE) {
        throw RuntimeException("Framebuffer is not complete")
    }
    GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0)

    fun render() {
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, fbo)
        GL46.glViewport(0, 0, width, height)
        GL46.glClearColor(0f, 0f, 1f, 1f)
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT or GL46.GL_DEPTH_BUFFER_BIT)
//        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0)
//        GL46.glBindFramebuffer(GL46.GL_READ_FRAMEBUFFER, fbo)
        val pixels = MemoryUtil.memAllocInt(width * height * 4)
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0)
        GL46.glFinish()
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, colorAttachment)
        GL46.glGetTexImage(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGBA, GL46.GL_UNSIGNED_INT, pixels)
        GL46.glFinish()

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val pixelArr = IntArray(width * height * 4)
        pixels.get(pixelArr)
        image.raster.setPixels(0, 0, width, height, pixelArr)
//        Path("image.png").outputStream().use {
//            PngWriter().write(AwtImage(image), ImageMetadata.empty, it)
//        }
        MemoryUtil.memFree(pixels)

        GLFW.glfwSwapBuffers(windowHandle)
    }

    GLFW.glfwSetWindowSizeCallback(windowHandle) { _, windowWidth, windowHeight ->
        width = windowWidth
        height = windowHeight

        GL46.glDeleteFramebuffers(fbo)
        GL46.glDeleteTextures(colorAttachment)
        GL46.glDeleteRenderbuffers(rbo)

        colorAttachment = GL46.glGenTextures()
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, colorAttachment)
        GL46.glTexImage2D(
            GL46.GL_TEXTURE_2D,
            0,
            GL46.GL_RGBA,
            width,
            height,
            0,
            GL46.GL_RGBA,
            GL46.GL_UNSIGNED_BYTE,
            MemoryUtil.NULL
        )

        rbo = GL46.glGenRenderbuffers()
        GL46.glBindRenderbuffer(GL46.GL_RENDERBUFFER, rbo)
        GL46.glRenderbufferStorage(GL46.GL_RENDERBUFFER, GL46.GL_DEPTH24_STENCIL8, width, height)

        fbo = GL46.glGenFramebuffers()
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, fbo)
        GL46.glFramebufferTexture2D(
            GL46.GL_FRAMEBUFFER,
            GL46.GL_COLOR_ATTACHMENT0,
            GL46.GL_TEXTURE_2D,
            colorAttachment,
            0
        )
        GL46.glFramebufferRenderbuffer(GL46.GL_FRAMEBUFFER, GL46.GL_DEPTH_STENCIL_ATTACHMENT, GL46.GL_RENDERBUFFER, rbo)
        GL46.glDrawBuffer(GL46.GL_COLOR_ATTACHMENT0)
        if (GL46.glCheckFramebufferStatus(GL46.GL_FRAMEBUFFER) != GL46.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Framebuffer is not complete")
        }
        GL46.glBindFramebuffer(GL46.GL_FRAMEBUFFER, 0)

        GLFW.glfwSwapInterval(0)
        render()
        GLFW.glfwSwapInterval(1)
    }
    GLFW.glfwShowWindow(windowHandle)

    while (!GLFW.glfwWindowShouldClose(windowHandle)) {
        render()
        GLFW.glfwPollEvents()
    }

    GLFW.glfwDestroyWindow(windowHandle)

    exitProcess(0)
}

private fun glfwGetWindowContentScale(window: Long): Float {
    val array = FloatArray(1)
    glfwGetWindowContentScale(window, array, FloatArray(1))
    return array[0]
}
