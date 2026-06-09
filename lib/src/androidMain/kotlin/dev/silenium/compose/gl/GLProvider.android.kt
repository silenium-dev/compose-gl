package dev.silenium.compose.gl

import android.opengl.GLES32
import java.nio.ByteBuffer

actual object GLProvider {
    // General
    actual fun glGetInteger(name: Int): Int {
        val result = IntArray(1)
        GLES32.glGetIntegerv(name, result, 0)
        return result[0]
    }

    actual fun glGetError(): Int = GLES32.glGetError()
    actual fun glViewport(x: Int, y: Int, width: Int, height: Int) =
        GLES32.glViewport(x, y, width, height)

    actual fun glFlush() = GLES32.glFlush()
    actual fun glDisable(cap: Int) = GLES32.glDisable(cap)

    // FBOs
    actual fun glGenFramebuffers(): Int {
        val result = IntArray(1)
        GLES32.glGenFramebuffers(1, result, 0)
        return result[0]
    }

    actual fun glBindFramebuffer(target: Int, framebuffer: Int) =
        GLES32.glBindFramebuffer(target, framebuffer)

    actual fun glDeleteFramebuffers(framebuffer: Int) =
        GLES32.glDeleteFramebuffers(1, intArrayOf(framebuffer), 0)

    actual fun glFramebufferTexture2D(
        target: Int,
        attachment: Int,
        textureTarget: Int,
        texture: Int,
        level: Int
    ) = GLES32.glFramebufferTexture2D(target, attachment, textureTarget, texture, level)

    actual fun glFramebufferRenderbuffer(
        target: Int,
        attachment: Int,
        renderbufferTarget: Int,
        renderbuffer: Int
    ) = GLES32.glFramebufferRenderbuffer(target, attachment, renderbufferTarget, renderbuffer)

    actual fun glCheckFramebufferStatus(target: Int): Int = GLES32.glCheckFramebufferStatus(target)

    // Textures
    actual fun glGenTextures(): Int {
        val result = IntArray(1)
        GLES32.glGenTextures(1, result, 0)
        return result[0]
    }

    actual fun glBindTexture(target: Int, texture: Int) = GLES32.glBindTexture(target, texture)
    actual fun glDeleteTextures(texture: Int) = GLES32.glDeleteTextures(1, intArrayOf(texture), 0)
    actual fun glTexParameteri(target: Int, name: Int, value: Int) =
        GLES32.glTexParameteri(target, name, value)

    actual fun glTexImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        border: Int,
        format: Int,
        type: Int,
        pixels: ByteBuffer?
    ) = GLES32.glTexImage2D(
        target,
        level,
        internalFormat,
        width,
        height,
        border,
        format,
        type,
        pixels,
    )

    // Renderbuffers
    actual fun glGenRenderbuffers(): Int {
        val result = IntArray(1)
        GLES32.glGenRenderbuffers(1, result, 0)
        return result[0]
    }

    actual fun glBindRenderbuffer(target: Int, renderbuffer: Int) =
        GLES32.glBindRenderbuffer(target, renderbuffer)

    actual fun glDeleteRenderbuffers(renderbuffer: Int) =
        GLES32.glDeleteRenderbuffers(1, intArrayOf(renderbuffer), 0)

    actual fun glRenderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int) =
        GLES32.glRenderbufferStorage(target, internalFormat, width, height)


    // Constants
    actual val GL_NO_ERROR: Int = GLES32.GL_NO_ERROR

    actual val GL_FRAMEBUFFER: Int = GLES32.GL_FRAMEBUFFER
    actual val GL_RENDERBUFFER: Int = GLES32.GL_RENDERBUFFER
    actual val GL_TEXTURE_2D: Int = GLES32.GL_TEXTURE_2D
    actual val GL_TEXTURE_2D_ARRAY: Int = GLES32.GL_TEXTURE_2D_ARRAY
    actual val GL_TEXTURE_3D: Int = GLES32.GL_TEXTURE_3D
    actual val GL_TEXTURE_CUBE_MAP: Int = GLES32.GL_TEXTURE_CUBE_MAP
    actual val GL_TEXTURE_CUBE_MAP_ARRAY: Int = GLES32.GL_TEXTURE_CUBE_MAP_ARRAY
    actual val GL_TEXTURE_BUFFER: Int = GLES32.GL_TEXTURE_BUFFER
    actual val GL_TEXTURE_2D_MULTISAMPLE: Int = GLES32.GL_TEXTURE_2D_MULTISAMPLE
    actual val GL_TEXTURE_2D_MULTISAMPLE_ARRAY: Int = GLES32.GL_TEXTURE_2D_MULTISAMPLE_ARRAY

    actual val GL_FRAMEBUFFER_BINDING: Int = GLES32.GL_FRAMEBUFFER_BINDING
    actual val GL_RENDERBUFFER_BINDING: Int = GLES32.GL_RENDERBUFFER_BINDING
    actual val GL_TEXTURE_BINDING_2D: Int = GLES32.GL_TEXTURE_BINDING_2D
    actual val GL_TEXTURE_BINDING_2D_ARRAY: Int = GLES32.GL_TEXTURE_BINDING_2D_ARRAY
    actual val GL_TEXTURE_BINDING_3D: Int = GLES32.GL_TEXTURE_BINDING_3D
    actual val GL_TEXTURE_BINDING_CUBE_MAP: Int = GLES32.GL_TEXTURE_BINDING_CUBE_MAP
    actual val GL_TEXTURE_BINDING_CUBE_MAP_ARRAY: Int = GLES32.GL_TEXTURE_BINDING_CUBE_MAP_ARRAY
    actual val GL_TEXTURE_BINDING_BUFFER: Int = GLES32.GL_TEXTURE_BINDING_BUFFER
    actual val GL_TEXTURE_BINDING_2D_MULTISAMPLE: Int = GLES32.GL_TEXTURE_BINDING_2D_MULTISAMPLE
    actual val GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY: Int =
        GLES32.GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY

    actual val GL_TEXTURE_WRAP_S: Int = GLES32.GL_TEXTURE_WRAP_S
    actual val GL_TEXTURE_WRAP_T: Int = GLES32.GL_TEXTURE_WRAP_T
    actual val GL_TEXTURE_MIN_FILTER: Int = GLES32.GL_TEXTURE_MIN_FILTER
    actual val GL_TEXTURE_MAG_FILTER: Int = GLES32.GL_TEXTURE_MAG_FILTER

    actual val GL_RGBA: Int = GLES32.GL_RGBA
    actual val GL_UNSIGNED_BYTE: Int = GLES32.GL_UNSIGNED_BYTE

    actual val GL_COLOR_ATTACHMENT0: Int = GLES32.GL_COLOR_ATTACHMENT0
    actual val GL_DEPTH_STENCIL_ATTACHMENT: Int = GLES32.GL_DEPTH_STENCIL_ATTACHMENT
    actual val GL_FRAMEBUFFER_COMPLETE: Int = GLES32.GL_FRAMEBUFFER_COMPLETE

    actual val allGLFeatures = listOf(
        GLES32.GL_BLEND,
        GLES32.GL_CULL_FACE,
        GLES32.GL_DEPTH_TEST,
        GLES32.GL_DITHER,
        GLES32.GL_POLYGON_OFFSET_FILL,
        GLES32.GL_PRIMITIVE_RESTART_FIXED_INDEX,
        GLES32.GL_RASTERIZER_DISCARD,
        GLES32.GL_SAMPLE_ALPHA_TO_COVERAGE,
        GLES32.GL_SAMPLE_COVERAGE,
        GLES32.GL_SAMPLE_MASK,
        GLES32.GL_SCISSOR_TEST,
        GLES32.GL_STENCIL_TEST,
    )
}
