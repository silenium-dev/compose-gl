plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.compose") version "2.3.10"
    id("org.jetbrains.compose") version "1.11.0-alpha02"
    application
}

val composeGlVersion = "0.9.1"

val useParent: Boolean = project.ext.properties.getOrDefault("examples.use-parent", "true").toString().toBoolean()

dependencies {
    implementation(compose.desktop.currentOs)
    if (useParent) {
        implementation(project(":"))
    } else {
        implementation("dev.silenium.compose.gl:compose-gl:${composeGlVersion}")
        implementation("dev.silenium.compose.gl:compose-gl-natives-all:${composeGlVersion}")
    }
    implementation(libs.slf4j.api)
    runtimeOnly("ch.qos.logback:logback-classic:1.5.20")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("dev.silenium.compose.gl.examples.skia_gl.MainKt")
}
