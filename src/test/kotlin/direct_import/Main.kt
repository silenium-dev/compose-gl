package direct_import

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.*
import org.jetbrains.skiko.SkiaLayer
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_RGBA8
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import java.awt.Window
import java.io.File
import javax.imageio.ImageIO

fun main() = application {
    System.setProperty("skiko.renderApi", "OPENGL")
    var textureId: Int? = null
    var textureSize: Pair<Int, Int> = 0 to 0
    Window(
        ::exitApplication,
        title = "Direct Render Test",
    ) {
        val window = LocalWindow.current!!
        var directContext by remember { mutableStateOf<DirectContext?>(null) }
        LaunchedEffect(window) {
            withContext(Dispatchers.IO) {
                while (isActive) {
                    window.directContext()?.let {
                        directContext = it
                        return@withContext
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val context = directContext ?: return@Canvas
            if (textureId == null) {
                val img = "image.png"
                val (id, size) = loadTexture(File(img))
                textureId = id
                textureSize = size
            }
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)

            GL11.glEnable(GL_TEXTURE_2D)
            GL11.glBindTexture(GL_TEXTURE_2D, textureId)

            GL11.glBegin(GL11.GL_QUADS)
            GL11.glTexCoord2f(0f, 0f)
            GL11.glVertex2f(-1f, -1f)
            GL11.glTexCoord2f(1f, 0f)
            GL11.glVertex2f(1f, -1f)
            GL11.glTexCoord2f(1f, 1f)
            GL11.glVertex2f(1f, 1f)
            GL11.glTexCoord2f(0f, 1f)
            GL11.glVertex2f(-1f, 1f)
            GL11.glEnd()

            GL11.glDisable(GL_TEXTURE_2D)

            val backendTex = BackendTexture.makeGL(
                width = textureSize.first,
                height = textureSize.second,
                isMipmapped = false,
                textureId = textureId,
                textureTarget = GL_TEXTURE_2D,
                textureFormat = GL_RGBA8,
            )
            try {
                val image = Image.adoptTextureFrom(
                    context,
                    backendTex,
                    SurfaceOrigin.BOTTOM_LEFT,
                    ColorType.RGBA_8888,
                )
                drawContext.canvas.nativeCanvas.drawImage(image, 0f, 0f)
            } catch (_: Throwable) {
                var err = GL11.glGetError()
                while (err != GL11.GL_NO_ERROR) {
                    println("GL Error: $err")
                    err = GL11.glGetError()
                }
                throw RuntimeException(
                    "Failed to create Image from BackendTexture: $backendTex"
                )
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
val LocalWindow: CompositionLocal<Window?> by lazy {
    val clazz = Class.forName("androidx.compose.ui.window.LocalWindowKt")
    val method = clazz.getMethod("getLocalWindow")
    method.invoke(null) as CompositionLocal<Window?>
}

fun Window.directContext(): DirectContext? {
    fun Any.getFieldValue(fieldName: String): Any? {
        val field = this::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(this)
    }

    val composePanel = this.getFieldValue("composePanel")!!
    val composeContainer = composePanel.getFieldValue("_composeContainer")!!
    val mediator = composeContainer.getFieldValue("mediator")!!
    val contentComponent = mediator.let {
        val getter = it::class.java.getMethod("getContentComponent")
        getter.invoke(it) as SkiaLayer
    }
    val redrawer = contentComponent.let {
        val getter = it::class.java.getMethod("getRedrawer${'$'}skiko")
        getter.invoke(it)
    }
    val contextHandler = redrawer.getFieldValue("contextHandler")!!
    val surface = contextHandler.let {
        val getter = it::class.java.superclass.superclass.getDeclaredMethod("getSurface")
        getter.isAccessible = true
        getter.invoke(it) as? Surface
    }
    return surface?.recordingContext
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
    val textureID = GL11.glGenTextures()
    GL11.glBindTexture(GL_TEXTURE_2D, textureID)

    GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP)
    GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP)
    GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
    GL11.glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

    GL11.glTexImage2D(
        GL_TEXTURE_2D,
        0,
        GL11.GL_RGBA8,
        width,
        height,
        0,
        GL_RGBA,
        GL11.GL_UNSIGNED_BYTE,
        buffer
    )

    return textureID to (width to height)
}
