package dev.silenium.compose.gl

import java.nio.ByteBuffer

expect object GLProvider {
    // General
    fun glGetInteger(name: Int): Int
    fun glGetError(): Int
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glFlush()
    fun glDisable(cap: Int)

    // FBOs
    fun glGenFramebuffers(): Int
    fun glBindFramebuffer(target: Int, framebuffer: Int)
    fun glDeleteFramebuffers(framebuffer: Int)
    fun glFramebufferTexture2D(target: Int, attachment: Int, textureTarget: Int, texture: Int, level: Int)
    fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbufferTarget: Int, renderbuffer: Int)
    fun glCheckFramebufferStatus(target: Int): Int

    // Textures
    fun glGenTextures(): Int
    fun glBindTexture(target: Int, texture: Int)
    fun glDeleteTextures(texture: Int)
    fun glTexParameteri(target: Int, name: Int, value: Int)
    fun glTexImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        border: Int,
        format: Int,
        type: Int,
        pixels: ByteBuffer?
    )

    // Renderbuffers
    fun glGenRenderbuffers(): Int
    fun glBindRenderbuffer(target: Int, renderbuffer: Int)
    fun glDeleteRenderbuffers(renderbuffer: Int)
    fun glRenderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int)


    // Constants
    val GL_NO_ERROR: Int

    val GL_FRAMEBUFFER: Int
    val GL_RENDERBUFFER: Int
    val GL_TEXTURE_2D: Int
    val GL_TEXTURE_2D_ARRAY: Int
    val GL_TEXTURE_3D: Int
    val GL_TEXTURE_CUBE_MAP: Int
    val GL_TEXTURE_CUBE_MAP_ARRAY: Int
    val GL_TEXTURE_BUFFER: Int
    val GL_TEXTURE_2D_MULTISAMPLE: Int
    val GL_TEXTURE_2D_MULTISAMPLE_ARRAY: Int

    val GL_FRAMEBUFFER_BINDING: Int
    val GL_RENDERBUFFER_BINDING: Int
    val GL_TEXTURE_BINDING_2D: Int
    val GL_TEXTURE_BINDING_2D_ARRAY: Int
    val GL_TEXTURE_BINDING_3D: Int
    val GL_TEXTURE_BINDING_CUBE_MAP: Int
    val GL_TEXTURE_BINDING_CUBE_MAP_ARRAY: Int
    val GL_TEXTURE_BINDING_BUFFER: Int
    val GL_TEXTURE_BINDING_2D_MULTISAMPLE: Int
    val GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY: Int

    val GL_TEXTURE_WRAP_S: Int
    val GL_TEXTURE_WRAP_T: Int
    val GL_TEXTURE_MIN_FILTER: Int
    val GL_TEXTURE_MAG_FILTER: Int

    val GL_RGBA: Int
    val GL_UNSIGNED_BYTE: Int

    val GL_COLOR_ATTACHMENT0: Int
    val GL_DEPTH_STENCIL_ATTACHMENT: Int
    val GL_FRAMEBUFFER_COMPLETE: Int

    val allGLFeatures: List<Int>
}
