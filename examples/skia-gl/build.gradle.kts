plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.compose") version "2.2.20"
    id("org.jetbrains.compose") version "1.10.0-alpha02"
    application
}

val composeGlVersion = "2e851b0-dev"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("dev.silenium.compose.gl:compose-gl:${composeGlVersion}")
    implementation("dev.silenium.compose.gl:compose-gl-natives-all:${composeGlVersion}")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.19")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("dev.silenium.compose.gl.examples.skia_gl.MainKt")
}
