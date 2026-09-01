package dev.silenium.libs.gl

interface GLProcAddressProvider {
    fun getGlProcAddress(name: String): Long
}
