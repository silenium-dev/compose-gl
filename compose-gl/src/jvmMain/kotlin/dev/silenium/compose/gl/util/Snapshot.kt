package dev.silenium.compose.gl.util

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import java.awt.image.BufferedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.outputStream

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
