package direct_import

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.scene.PlatformLayersComposeScene
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.silenium.compose.gl.directContext
import dev.silenium.compose.gl.fbo.FBO
import dev.silenium.compose.gl.fbo.draw
import dev.silenium.compose.gl.graphicsApi
import dev.silenium.compose.gl.interop.D3DInterop
import dev.silenium.compose.gl.objects.Renderbuffer
import dev.silenium.compose.gl.objects.Texture
import dev.silenium.compose.gl.util.checkGLError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.*
import org.jetbrains.skiko.Version
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.EXTMemoryObject
import org.lwjgl.opengl.EXTMemoryObject.GL_OPTIMAL_TILING_EXT
import org.lwjgl.opengl.EXTMemoryObject.GL_TEXTURE_TILING_EXT
import org.lwjgl.opengl.EXTMemoryObjectWin32
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import java.io.File
import javax.imageio.ImageIO


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

@OptIn(ExperimentalUnsignedTypes::class, InternalComposeUiApi::class)
fun main() = application {
    System.setProperty("skiko.renderApi", "DIRECT3D")

    val classpath = System.getProperty("java.class.path")
    val classPathValues: Array<String> =
        classpath.split(File.pathSeparator.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    for (cpv in classPathValues) {
        println(cpv)
    }

    var directContext: DirectContext? by mutableStateOf(null)

    var d3dTexture: Long? = null
    var sharedHandle: Long? = null
    var backendTexture: BackendTexture? = null
    var glMemory: Int? = null
    var image: Image? = null
    var initialized by mutableStateOf(false)

    var glfwWindow = 0L
    var fbo: FBO? = null
    var glSurface: Surface? = null
    var glRenderTarget: BackendRenderTarget? = null
    var glDirectContext: DirectContext? = null

    val renderer = GLTextureDrawer()

    val glScene = PlatformLayersComposeScene()
    glScene.setContent {
        Text("Hello from Skia on OpenGL", style = MaterialTheme.typography.h2)
    }

    Window(
        ::exitApplication,
        title = "Direct Render Test",
    ) {
        Box(Modifier.fillMaxSize()) {
            DisposableEffect(Unit) {
                onDispose {
                    GLFW.glfwMakeContextCurrent(glfwWindow)
                    renderer.destroy()
                    glSurface?.close()
                    glRenderTarget?.close()
                    glDirectContext?.close()
                    fbo?.destroy()
                    image?.close()
                    glMemory?.let(EXTMemoryObject::glDeleteMemoryObjectsEXT)
                    d3dTexture?.let(D3DInterop::destroyTexture)
                    sharedHandle?.let(D3DInterop::closeSharedHandle)
                    GLFW.glfwMakeContextCurrent(MemoryUtil.NULL)
                    glfwWindow.let(GLFW::glfwDestroyWindow)
                    println("Disposed")
                }
            }
            LaunchedEffect(window) {
                withContext(Dispatchers.IO) {
                    while (directContext == null && isActive) {
                        directContext = window.directContext()
                    }
                }
            }
            Canvas(Modifier.matchParentSize()) {
                if (directContext == null) return@Canvas
                if (!initialized) {
                    GLFW.glfwInitHint(GLFW.GLFW_COCOA_MENUBAR, GLFW.GLFW_FALSE)
                    if (!GLFW.glfwInit()) {
                        throw RuntimeException("Failed to initialize GLFW")
                    }
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
                    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
                    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)

                    glfwWindow = GLFW.glfwCreateWindow(128, 128, "", MemoryUtil.NULL, MemoryUtil.NULL)
//                    println("glfwWindow: $glfwWindow")
                    if (glfwWindow == MemoryUtil.NULL) {
                        throw RuntimeException("Failed to create GLFW window")
                    }
                    GLFW.glfwMakeContextCurrent(glfwWindow)
                    GL.createCapabilities()
                    GLFW.glfwMakeContextCurrent(MemoryUtil.NULL)
                    initialized = true
                }
                GLFW.glfwMakeContextCurrent(glfwWindow)
                if (fbo?.size != size.toIntSize()) {
                    glMemory?.let(EXTMemoryObject::glDeleteMemoryObjectsEXT)
                    glMemory = null
                    sharedHandle?.let(D3DInterop::closeSharedHandle)
                    sharedHandle = null
                    d3dTexture?.let(D3DInterop::destroyTexture)
                    d3dTexture = null
                    backendTexture?.close()
                    backendTexture = null
                    glSurface?.close()
                    glSurface = null
                    glRenderTarget?.close()
                    glRenderTarget = null
                    glDirectContext?.close()
                    glDirectContext = null
                    image?.close()
                    image = null
                    fbo?.destroy()
                    fbo = null

                    d3dTexture = D3DInterop.createTexture(window, size.width.toInt(), size.height.toInt())
//                    println("d3dTexture: $d3dTexture")

                    sharedHandle = D3DInterop.exportSharedHandle(window, d3dTexture!!)
//                    println("sharedHandle: $sharedHandle")

                    val colorAttachment = Texture(glGenTextures(), size.toIntSize(), GL_TEXTURE_2D, GL_RGBA8)
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
//                    println("glMemory: $glMemory")
                    EXTMemoryObjectWin32.glImportMemoryWin32HandleEXT(
                        glMemory!!, size.width.toInt() * size.height.toInt() * 4 * 2L,
                        EXTMemoryObjectWin32.GL_HANDLE_TYPE_D3D12_RESOURCE_EXT, sharedHandle!!,
                    )
                    checkGLError("glImportMemoryWin32HandleEXT")

                    EXTMemoryObject.glTextureStorageMem2DEXT(
                        colorAttachment.id,
                        1, GL_RGBA8,
                        size.width.toInt(), size.height.toInt(),
                        glMemory!!, 0
                    )
                    checkGLError("glTextureStorageMem2DEXT")

                    val depthStencilAttachment = Renderbuffer.create(size.toIntSize(), GL_DEPTH24_STENCIL8)
                    fbo = FBO.create(colorAttachment, depthStencilAttachment)

                    backendTexture = D3DInterop.makeBackendTexture(d3dTexture!!)
                    image = Image.adoptTextureFrom(
                        directContext!!, backendTexture!!,
                        SurfaceOrigin.TOP_LEFT, ColorType.RGBA_8888,
                    )

                    glDirectContext = DirectContext.makeGL()
                    glRenderTarget = BackendRenderTarget.makeGL(
                        size.width.toInt(), size.height.toInt(), 1, 8, fbo!!.id, GL_RGBA8,
                    )
                    glSurface = Surface.makeFromBackendRenderTarget(
                        glDirectContext!!,
                        glRenderTarget!!,
                        SurfaceOrigin.TOP_LEFT,
                        SurfaceColorFormat.RGBA_8888,
                        ColorSpace.sRGB,
                    )
                }

                fbo?.draw {
                    println("redrawing (${size})")
                    renderer.render()
                }
                glFinish()
                glDirectContext!!.resetGLAll()
                with(glSurface!!.canvas) {
                    drawRect(Rect(100f, 200f, 200f, 300f), Paint().apply { color = Color.RED })
                    save()
                    translate(100f, 300f)
                    glScene.render(this.asComposeCanvas(), 0L)
                    restore()
                }
                glSurface!!.flushAndSubmit(true)
//                fbo?.snapshot(Path("rendered.png"))
                GLFW.glfwMakeContextCurrent(MemoryUtil.NULL)

                drawContext.canvas.nativeCanvas.drawImage(image!!, 0f, 0f)
            }
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.surface,
                modifier = Modifier.padding(8.dp).wrapContentWidth(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Skia Graphics API: ${window.graphicsApi()}")
                    Text("Skia Version: ${Version.skia}")
                    Text("Skiko Version: ${Version.skiko}")
                    Button(onClick = { print("button pressed") }) {
                        Text("Button")
                    }
                }
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
