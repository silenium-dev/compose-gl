package dev.silenium.compose.gl.canvas

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.fbo.FBO
import org.jetbrains.skia.DirectContext
import org.lwjgl.opengl.GL46.*
import java.awt.Window
import kotlin.time.Duration

interface CanvasDriver {
    fun setup(directContext: DirectContext)
    fun render(
        scope: DrawScope,
        userResizeHandler: FBOScope.(old: IntSize?, new: IntSize) -> Unit = { _, _ -> },
        block: FBOScope.() -> Unit,
    )

    fun display(scope: DrawScope)
    fun dispose(userDisposeHandler: () -> Unit)
}

fun interface CanvasDriverFactory<T : CanvasDriver> {
    fun create(window: Window): T
}

interface FBOScope {
    val fbo: FBO
}

interface GLDrawScope : FBOScope {
    val deltaTime: Duration
}

data class GLDrawScopeImpl(
    val fboScope: FBOScope,
    override val deltaTime: Duration,
) : FBOScope by fboScope, GLDrawScope

data class FBOScopeImpl(override val fbo: FBO) : FBOScope

internal inline fun <T> FBOScope.drawGL(block: () -> T): T {
    fbo.bind()
    resetGLFeatures()
    try {
        return block()
    } finally {
        fbo.unbind()
        glFlush()
    }
}

fun resetGLFeatures() {
    listOf(
        GL_BLEND,
        GL_CLIP_DISTANCE0,
        GL_CLIP_DISTANCE1,
        GL_CLIP_DISTANCE2,
        GL_CLIP_DISTANCE3,
        GL_CLIP_DISTANCE4,
        GL_CLIP_DISTANCE5,
        GL_CLIP_DISTANCE6,
        GL_CLIP_DISTANCE7,
        GL_COLOR_LOGIC_OP,
        GL_CULL_FACE,
        GL_DEBUG_OUTPUT,
        GL_DEBUG_OUTPUT_SYNCHRONOUS,
        GL_DEPTH_CLAMP,
        GL_DEPTH_TEST,
        GL_DITHER,
        GL_FRAMEBUFFER_SRGB,
        GL_LINE_SMOOTH,
        GL_MULTISAMPLE,
        GL_POLYGON_OFFSET_FILL,
        GL_POLYGON_OFFSET_LINE,
        GL_POLYGON_OFFSET_POINT,
        GL_POLYGON_SMOOTH,
        GL_PRIMITIVE_RESTART,
        GL_PRIMITIVE_RESTART_FIXED_INDEX,
        GL_RASTERIZER_DISCARD,
        GL_SAMPLE_ALPHA_TO_COVERAGE,
        GL_SAMPLE_ALPHA_TO_ONE,
        GL_SAMPLE_COVERAGE,
        GL_SAMPLE_SHADING,
        GL_SAMPLE_MASK,
        GL_SCISSOR_TEST,
        GL_STENCIL_TEST,
        GL_TEXTURE_CUBE_MAP_SEAMLESS,
        GL_PROGRAM_POINT_SIZE,
    )
    glDisable(GL_BLEND)
}
