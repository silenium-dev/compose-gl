plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.compose") version "2.2.20"
    id("org.jetbrains.compose") version "1.10.0-alpha02"
    application
}

val composeGlVersion = "2e851b0-dev"

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
