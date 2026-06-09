package dev.silenium.compose.gl.objects

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.GLProvider.GL_RENDERBUFFER
import dev.silenium.compose.gl.GLProvider.GL_RENDERBUFFER_BINDING
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_2D
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_2D_ARRAY
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_2D_MULTISAMPLE
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_2D_MULTISAMPLE_ARRAY
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_3D
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_BINDING_2D
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_BINDING_2D_ARRAY
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_BINDING_2D_MULTISAMPLE
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_BINDING_3D
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_BINDING_BUFFER
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_BINDING_CUBE_MAP
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_BINDING_CUBE_MAP_ARRAY
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_BUFFER
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_CUBE_MAP
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_CUBE_MAP_ARRAY

interface TextureOrRenderbuffer<T : TextureOrRenderbuffer<T>> {
    val id: Int
    val target: Int
    val binding: Int
    val size: IntSize
    val internalFormat: Int

    fun bind()
    fun unbind()
    fun destroy()

    /**
     * Returns the resized renderbuffer and abandons this.
     */
    fun resize(size: IntSize): T

    companion object {
        val textureTargetBindings = mapOf(
            GL_TEXTURE_2D to GL_TEXTURE_BINDING_2D,
            GL_TEXTURE_2D_ARRAY to GL_TEXTURE_BINDING_2D_ARRAY,
            GL_TEXTURE_3D to GL_TEXTURE_BINDING_3D,
            GL_TEXTURE_CUBE_MAP to GL_TEXTURE_BINDING_CUBE_MAP,
            GL_TEXTURE_CUBE_MAP_ARRAY to GL_TEXTURE_BINDING_CUBE_MAP_ARRAY,
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
