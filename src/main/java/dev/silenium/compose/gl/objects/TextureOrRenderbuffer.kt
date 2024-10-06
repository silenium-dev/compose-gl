package dev.silenium.compose.gl.objects

import androidx.compose.ui.unit.IntSize
import org.lwjgl.opengl.GL46.*

interface TextureOrRenderbuffer {
    val id: Int
    val target: Int
    val binding: Int
    val size: IntSize
    val internalFormat: Int

    fun bind()
    fun unbind()
    fun destroy()

    companion object {
        val textureTargetBindings = mapOf(
            GL_TEXTURE_1D to GL_TEXTURE_BINDING_1D,
            GL_TEXTURE_2D to GL_TEXTURE_BINDING_2D,
            GL_TEXTURE_2D_ARRAY to GL_TEXTURE_BINDING_2D_ARRAY,
            GL_TEXTURE_3D to GL_TEXTURE_BINDING_3D,
            GL_TEXTURE_CUBE_MAP to GL_TEXTURE_BINDING_CUBE_MAP,
            GL_TEXTURE_CUBE_MAP_ARRAY to GL_TEXTURE_BINDING_CUBE_MAP_ARRAY,
            GL_TEXTURE_RECTANGLE to GL_TEXTURE_BINDING_RECTANGLE,
            GL_TEXTURE_BUFFER to GL_TEXTURE_BINDING_BUFFER,
            GL_TEXTURE_2D_MULTISAMPLE to GL_TEXTURE_BINDING_2D_MULTISAMPLE,
            GL_TEXTURE_2D_MULTISAMPLE_ARRAY to GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY,
        )

        val renderbufferBindings = mapOf(
            GL_RENDERBUFFER to GL_RENDERBUFFER_BINDING,
        )

        val targetBindings = textureTargetBindings + renderbufferBindings
    }
}
