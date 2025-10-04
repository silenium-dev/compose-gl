package direct_import

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.silenium.compose.gl.direct.GLDirectCanvas
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random

//language=glsl
const val VERTEX_SHADER_SOURCE = """
#version 330 core

// Input vertex data, different for all executions of this shader.
layout(location = 0) in vec3 vertexPosition;
layout(location = 1) in vec2 vertexUV;

// Output data ; will be interpolated for each fragment.
out vec2 UV;

void main(){
    // Output position of the vertex, in clip space : MVP * position
    gl_Position = vec4(vertexPosition,1);

    // UV of the vertex. No special space for this one.
    UV = vertexUV;
}
"""

//language=glsl
const val FRAGMENT_SHADER_SOURCE = """
#version 330 core

// Interpolated values from the vertex shaders
in vec2 UV;

// Output data
out vec4 color;

// Values that stay constant for the whole mesh.
uniform sampler2D myTextureSampler;

void main(){

    // Output color = color of the texture at the specified UV
    color = vec4(texture( myTextureSampler, UV ).rgb, 1.0);
//    color = vec4(UV.xy, 0.0, 1.0);
}
"""

@OptIn(ExperimentalUnsignedTypes::class)
fun main() = application {
    System.setProperty("skiko.renderApi", "OPENGL")
    var textureId = 0
    var initialized = false
    var shaderProgram = 0
    var vao = 0
    var vbo = 0
    var ibo = 0
    Window(
        ::exitApplication,
        title = "Direct Render Test",
    ) {
        Box(Modifier.fillMaxSize()) {
            DisposableEffect(Unit) {
                onDispose {
                    glDeleteProgram(shaderProgram)
                    glDeleteVertexArrays(vao)
                    glDeleteBuffers(vbo)
                    glDeleteBuffers(ibo)
                    glDeleteTextures(textureId)
                    GL.destroy()
                    initialized = false
                    shaderProgram = 0
                    vao = 0
                    vbo = 0
                    ibo = 0
                    textureId = 0
                    println("Disposed")
                }
            }
            GLDirectCanvas(Modifier.matchParentSize()) {
                if (!initialized) {
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

                println("redrawing (${fbo.size})")
                glEnable(GL_BLEND)
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

                glClearColor(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1f)
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

                glFinish()
            }
            Button(onClick = { println("button pressed") }) {
                Text("Button")
            }
        }
    }
}

fun loadTexture(file: File): Pair<Int, Pair<Int, Int>> {
    val image = ImageIO.read(file)

    val width = image.width
    val height = image.height

    // Convert image to RGBA
    val pixels = IntArray(width * height)
    image.getRGB(0, 0, width, height, pixels, 0, width)

    val buffer = BufferUtils.createByteBuffer(width * height * 4)

    // OpenGL expects bottom-to-top, so flip vertically
    for (y in height - 1 downTo 0) {
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
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

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

    return textureID to (width to height)
}
