package dev.silenium.compose.gl.fbo

class NoRenderFBOAvailable(message: String) : Exception(message) {
    constructor() : this("No render FBO available.")
}
