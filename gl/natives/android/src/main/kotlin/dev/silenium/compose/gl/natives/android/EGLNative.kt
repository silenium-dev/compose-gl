package dev.silenium.compose.gl.natives.android

object EGLNative {
    external fun eglGetProcAddress(procname: String): Long
    init {
        System.loadLibrary("compose-gl")
    }
}
