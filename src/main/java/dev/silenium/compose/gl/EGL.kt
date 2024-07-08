package dev.silenium.compose.gl

import dev.silenium.compose.gl.util.Natives

object EGL {
    init {
        Natives.load("libcompose-gl.so")
    }

    external fun debugContext()
    external fun stringFromJNI(): String
}
