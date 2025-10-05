package direct_import

import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_FALSE
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glClear
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glDeleteTextures
import org.lwjgl.opengl.GL11.glDisable
import org.lwjgl.opengl.GL11.glDrawElements
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_STATIC_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glDeleteBuffers
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.GL_COMPILE_STATUS
import org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER
import org.lwjgl.opengl.GL20.GL_LINK_STATUS
import org.lwjgl.opengl.GL20.GL_VERTEX_SHADER
import org.lwjgl.opengl.GL20.glAttachShader
import org.lwjgl.opengl.GL20.glCompileShader
import org.lwjgl.opengl.GL20.glCreateProgram
import org.lwjgl.opengl.GL20.glCreateShader
import org.lwjgl.opengl.GL20.glDeleteProgram
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glGetProgramInfoLog
import org.lwjgl.opengl.GL20.glGetProgrami
import org.lwjgl.opengl.GL20.glGetShaderInfoLog
import org.lwjgl.opengl.GL20.glGetShaderi
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glLinkProgram
import org.lwjgl.opengl.GL20.glShaderSource
import org.lwjgl.opengl.GL20.glUniform1i
import org.lwjgl.opengl.GL20.glUseProgram
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glDeleteVertexArrays
import org.lwjgl.opengl.GL30.glGenVertexArrays
import java.io.File
import kotlin.random.Random

class GLTextureDrawer {
    private var textureId = 0
    private var initialized = false
    private var shaderProgram = 0
    private var vao = 0
    private var vbo = 0
    private var ibo = 0

    fun initialize() {
        if (initialized) return

        val img = "image.png"
        val (id, size) = loadTexture(File(img))
        textureId = id

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

    fun render() {
        initialize()

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glClearColor(0f, .5f, .5f, 1f)
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
