package direct

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL30.*
import java.io.InputStream
import javax.imageio.ImageIO

//language=glsl
const val VERTEX_SHADER_SOURCE = """#version 330 core

layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec2 vertexUV;

out vec2 UV;

void main() {
    gl_Position = vec4(vertexPosition, 1);
    UV = vertexUV;
}
"""

//language=glsl
const val FRAGMENT_SHADER_SOURCE = """#version 330 core

in vec2 UV;
out vec4 color;

uniform sampler2D myTextureSampler;

void main() {
    color = vec4(texture(myTextureSampler, UV).rgb, 1.0);
    //color = vec4(UV.xy, 0.0, 1.0);
}
"""

class SampleRenderer {
    private var textureId = 0
    private var initialized = false
    private var shaderProgram = 0
    private var vao = 0
    private var vbo = 0
    private var ibo = 0

    private fun initialize() {
        if (initialized) return

        val img = "image.png"
        val stream = javaClass.classLoader.getResourceAsStream(img) ?: error("Resource not found: $img")
        textureId = loadTexture(stream)

        shaderProgram = glCreateProgram()
        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(vertexShader, VERTEX_SHADER_SOURCE)
        glCompileShader(vertexShader)
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == GL_FALSE) {
            println("Vertex shader compilation failed: ${glGetShaderInfoLog(vertexShader)}")
        }
        glShaderSource(fragmentShader, FRAGMENT_SHADER_SOURCE)
        glCompileShader(fragmentShader)
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == GL_FALSE) {
            println("Fragment shader compilation failed: ${glGetShaderInfoLog(fragmentShader)}")
        }
        glAttachShader(shaderProgram, vertexShader)
        glAttachShader(shaderProgram, fragmentShader)
        glLinkProgram(shaderProgram)
        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            println("Shader program linking failed: ${glGetProgramInfoLog(shaderProgram)}")
        }
        glUseProgram(shaderProgram)
        val loc = glGetUniformLocation(shaderProgram, "myTextureSampler")
        glUniform1i(loc, 0)
        glUseProgram(0)

        val vertices = floatArrayOf(
            //       aPosition     | aTexCoords
            0.5f, 0.5f, 0.0f, 1.0f, 1.0f,
            0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
            -0.5f, -0.5f, 0.0f, 0.0f, 0.0f,
            -0.5f, 0.5f, 0.0f, 0.0f, 1.0f
        )
        val indices = intArrayOf(
            0, 1, 3,
            1, 2, 3
        )
        vao = glGenVertexArrays()
        glBindVertexArray(vao)

        vbo = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

        ibo = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.SIZE_BYTES, 3L * Float.SIZE_BYTES)
        glEnableVertexAttribArray(1)
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        initialized = true
    }

    fun draw() {
        initialize()

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glClearColor(0f, .8f, .4f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)

        glBindVertexArray(vao)
        glUseProgram(shaderProgram)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)

        glBindTexture(GL_TEXTURE_2D, 0)
        glBindVertexArray(0)
        glUseProgram(0)
        glDisable(GL_BLEND)
    }

    fun destroy() {
        glDeleteProgram(shaderProgram)
        glDeleteVertexArrays(vao)
        glDeleteBuffers(vbo)
        glDeleteBuffers(ibo)
        glDeleteTextures(textureId)
        initialized = false
        shaderProgram = 0
        vao = 0
        vbo = 0
        ibo = 0
        textureId = 0
    }
}

fun loadTexture(stream: InputStream): Int {
    val image = ImageIO.read(stream)

    val width = image.width
    val height = image.height

    // Convert image to RGBA
    val pixels = IntArray(width * height)
    image.getRGB(0, 0, width, height, pixels, 0, width)

    val buffer = BufferUtils.createByteBuffer(width * height * 4)

    // OpenGL expects bottom-to-top, so flip vertically
    for (y in 0..<height) {
        for (x in 0..<width) {
            val pixel = pixels[y * width + x]
            buffer.put(((pixel shr 16) and 0xFF).toByte()) // Red
            buffer.put(((pixel shr 8) and 0xFF).toByte()) // Green
            buffer.put((pixel and 0xFF).toByte()) // Blue
            buffer.put(((pixel shr 24) and 0xFF).toByte()) // Alpha
        }
    }

    buffer.flip()

    GL.createCapabilities()
    val textureID = glGenTextures()
    glBindTexture(GL_TEXTURE_2D, textureID)

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

    glTexImage2D(
        GL_TEXTURE_2D,
        0,
        GL_RGBA8,
        width,
        height,
        0,
        GL_RGBA,
        GL_UNSIGNED_BYTE,
        buffer
    )
    glBindTexture(GL_TEXTURE_2D, 0)

    return textureID
}
