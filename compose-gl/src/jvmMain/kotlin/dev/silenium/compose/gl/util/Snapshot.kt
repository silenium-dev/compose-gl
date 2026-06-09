package dev.silenium.compose.gl.util

import dev.silenium.compose.gl.fbo.FBO
import org.jetbrains.skia.*
import org.lwjgl.opengl.GL30.*
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.outputStream

fun FBO.snapshot(target: Path) {
    val prevReadFbo = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)

    glBindFramebuffer(GL_READ_FRAMEBUFFER, id)
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

fun Bitmap.snapshot(target: Path) {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val pixelArr = readPixels() ?: error("Failed to read pixels")
    image.raster.setPixels(0, 0, width, height, pixelArr.map { it.toInt() }.toIntArray())
    target.outputStream().use { ImageIO.write(image, "png", it) }
}

fun Surface.snapshot(target: Path) {
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
    readPixels(bitmap, 0, 0)
    bitmap.snapshot(target)
    bitmap.close()
}
