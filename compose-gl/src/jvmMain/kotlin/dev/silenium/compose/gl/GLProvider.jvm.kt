package dev.silenium.compose.gl

import org.lwjgl.opengl.GL32
import org.lwjgl.opengl.GL40
import org.lwjgl.opengl.GL43
import java.nio.ByteBuffer

internal actual object GLProvider {
    // General
    actual fun glGetInteger(name: Int): Int = GL32.glGetInteger(name)

    actual fun glGetError(): Int = GL32.glGetError()
    actual fun glViewport(x: Int, y: Int, width: Int, height: Int) =
        GL32.glViewport(x, y, width, height)

    actual fun glFlush() = GL32.glFlush()
    actual fun glDisable(cap: Int) = GL32.glDisable(cap)

    // FBOs
    actual fun glGenFramebuffers(): Int = GL32.glGenFramebuffers()

    actual fun glBindFramebuffer(target: Int, framebuffer: Int) =
        GL32.glBindFramebuffer(target, framebuffer)

    actual fun glDeleteFramebuffers(framebuffer: Int) = GL32.glDeleteFramebuffers(framebuffer)

    actual fun glFramebufferTexture2D(
        target: Int,
        attachment: Int,
        textureTarget: Int,
        texture: Int,
        level: Int
    ) = GL32.glFramebufferTexture2D(target, attachment, textureTarget, texture, level)

    actual fun glFramebufferRenderbuffer(
        target: Int,
        attachment: Int,
        renderbufferTarget: Int,
        renderbuffer: Int
    ) = GL32.glFramebufferRenderbuffer(target, attachment, renderbufferTarget, renderbuffer)

    actual fun glCheckFramebufferStatus(target: Int): Int = GL32.glCheckFramebufferStatus(target)

    // Textures
    actual fun glGenTextures(): Int = GL32.glGenTextures()

    actual fun glBindTexture(target: Int, texture: Int) = GL32.glBindTexture(target, texture)
    actual fun glDeleteTextures(texture: Int) = GL32.glDeleteTextures(texture)
    actual fun glTexParameteri(target: Int, name: Int, value: Int) =
        GL32.glTexParameteri(target, name, value)

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
    ) = GL32.glTexImage2D(
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
    actual fun glGenRenderbuffers(): Int = GL32.glGenRenderbuffers()

    actual fun glBindRenderbuffer(target: Int, renderbuffer: Int) =
        GL32.glBindRenderbuffer(target, renderbuffer)

    actual fun glDeleteRenderbuffers(renderbuffer: Int) = GL32.glDeleteRenderbuffers(renderbuffer)

    actual fun glRenderbufferStorage(target: Int, internalFormat: Int, width: Int, height: Int) =
        GL32.glRenderbufferStorage(target, internalFormat, width, height)


    // Constants
    actual val GL_NO_ERROR: Int = GL32.GL_NO_ERROR

    actual val GL_FRAMEBUFFER: Int = GL32.GL_FRAMEBUFFER
    actual val GL_RENDERBUFFER: Int = GL32.GL_RENDERBUFFER
    actual val GL_TEXTURE_2D: Int = GL32.GL_TEXTURE_2D
    actual val GL_TEXTURE_2D_ARRAY: Int = GL32.GL_TEXTURE_2D_ARRAY
    actual val GL_TEXTURE_3D: Int = GL32.GL_TEXTURE_3D
    actual val GL_TEXTURE_CUBE_MAP: Int = GL32.GL_TEXTURE_CUBE_MAP
    actual val GL_TEXTURE_CUBE_MAP_ARRAY: Int = GL40.GL_TEXTURE_CUBE_MAP_ARRAY
    actual val GL_TEXTURE_BUFFER: Int = GL32.GL_TEXTURE_BUFFER
    actual val GL_TEXTURE_2D_MULTISAMPLE: Int = GL32.GL_TEXTURE_2D_MULTISAMPLE
    actual val GL_TEXTURE_2D_MULTISAMPLE_ARRAY: Int = GL32.GL_TEXTURE_2D_MULTISAMPLE_ARRAY

    actual val GL_FRAMEBUFFER_BINDING: Int = GL32.GL_FRAMEBUFFER_BINDING
    actual val GL_RENDERBUFFER_BINDING: Int = GL32.GL_RENDERBUFFER_BINDING
    actual val GL_TEXTURE_BINDING_2D: Int = GL32.GL_TEXTURE_BINDING_2D
    actual val GL_TEXTURE_BINDING_2D_ARRAY: Int = GL32.GL_TEXTURE_BINDING_2D_ARRAY
    actual val GL_TEXTURE_BINDING_3D: Int = GL32.GL_TEXTURE_BINDING_3D
    actual val GL_TEXTURE_BINDING_CUBE_MAP: Int = GL32.GL_TEXTURE_BINDING_CUBE_MAP
    actual val GL_TEXTURE_BINDING_CUBE_MAP_ARRAY: Int = GL40.GL_TEXTURE_BINDING_CUBE_MAP_ARRAY
    actual val GL_TEXTURE_BINDING_BUFFER: Int = GL32.GL_TEXTURE_BINDING_BUFFER
    actual val GL_TEXTURE_BINDING_2D_MULTISAMPLE: Int = GL32.GL_TEXTURE_BINDING_2D_MULTISAMPLE
    actual val GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY: Int =
        GL32.GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY

    actual val GL_TEXTURE_WRAP_S: Int = GL32.GL_TEXTURE_WRAP_S
    actual val GL_TEXTURE_WRAP_T: Int = GL32.GL_TEXTURE_WRAP_T
    actual val GL_TEXTURE_MIN_FILTER: Int = GL32.GL_TEXTURE_MIN_FILTER
    actual val GL_TEXTURE_MAG_FILTER: Int = GL32.GL_TEXTURE_MAG_FILTER

    actual val GL_RGBA: Int = GL32.GL_RGBA
    actual val GL_UNSIGNED_BYTE: Int = GL32.GL_UNSIGNED_BYTE

    actual val GL_COLOR_ATTACHMENT0: Int = GL32.GL_COLOR_ATTACHMENT0
    actual val GL_DEPTH_STENCIL_ATTACHMENT: Int = GL32.GL_DEPTH_STENCIL_ATTACHMENT
    actual val GL_FRAMEBUFFER_COMPLETE: Int = GL32.GL_FRAMEBUFFER_COMPLETE

    actual val allGLFeatures = listOf(
        GL32.GL_BLEND,
        GL32.GL_CLIP_DISTANCE0,
        GL32.GL_CLIP_DISTANCE1,
        GL32.GL_CLIP_DISTANCE2,
        GL32.GL_CLIP_DISTANCE3,
        GL32.GL_CLIP_DISTANCE4,
        GL32.GL_CLIP_DISTANCE5,
        GL32.GL_CLIP_DISTANCE6,
        GL32.GL_CLIP_DISTANCE7,
        GL32.GL_COLOR_LOGIC_OP,
        GL32.GL_CULL_FACE,
        GL43.GL_DEBUG_OUTPUT,
        GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS,
        GL32.GL_DEPTH_CLAMP,
        GL32.GL_DEPTH_TEST,
        GL32.GL_DITHER,
        GL32.GL_FRAMEBUFFER_SRGB,
        GL32.GL_LINE_SMOOTH,
        GL32.GL_MULTISAMPLE,
        GL32.GL_POLYGON_OFFSET_FILL,
        GL32.GL_POLYGON_OFFSET_LINE,
        GL32.GL_POLYGON_OFFSET_POINT,
        GL32.GL_POLYGON_SMOOTH,
        GL32.GL_PRIMITIVE_RESTART,
        GL43.GL_PRIMITIVE_RESTART_FIXED_INDEX,
        GL32.GL_RASTERIZER_DISCARD,
        GL32.GL_SAMPLE_ALPHA_TO_COVERAGE,
        GL32.GL_SAMPLE_ALPHA_TO_ONE,
        GL32.GL_SAMPLE_COVERAGE,
        GL40.GL_SAMPLE_SHADING,
        GL32.GL_SAMPLE_MASK,
        GL32.GL_SCISSOR_TEST,
        GL32.GL_STENCIL_TEST,
        GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS,
        GL32.GL_PROGRAM_POINT_SIZE,
    )
}
