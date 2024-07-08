package dev.silenium.compose.gl.util

import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.Bitmap
import org.lwjgl.opengles.GLES32.*
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.outputStream

fun snapshot(fbo: Int, size: IntSize, target: Path) {
    val prevReadFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)

    glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo)
    val pixels = MemoryUtil.memAlloc(size.width * size.height * 4)
    glReadPixels(0, 0, size.width, size.height, GL_RGBA, GL_UNSIGNED_BYTE, pixels)
    glFinish()

    val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB)
    val pixelArr = ByteArray(size.width * size.height * 4)
    pixels.get(pixelArr)
    image.raster.setPixels(0, 0, size.width, size.height, pixelArr.map { it.toInt() }.toIntArray())
    target.outputStream().use { ImageIO.write(image, "png", it) }
    MemoryUtil.memFree(pixels)

    glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFbo)
}

fun snapshot(bitmap: Bitmap, target: Path) {
    val image = BufferedImage(bitmap.width, bitmap.height, BufferedImage.TYPE_INT_ARGB)
    val pixelArr = bitmap.readPixels() ?: error("Failed to read pixels")
    image.raster.setPixels(0, 0, bitmap.width, bitmap.height, pixelArr.map { it.toInt() }.toIntArray())
    target.outputStream().use { ImageIO.write(image, "png", it) }
}
