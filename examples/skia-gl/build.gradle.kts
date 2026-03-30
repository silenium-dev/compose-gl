plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

val composeGlVersion = "0.11.0-rc.3"

val useParent: Boolean = project.ext.properties.getOrDefault("examples.use-parent", "true").toString().toBoolean()

dependencies {
    implementation(compose.desktop.currentOs)
    if (useParent) {
        implementation(project(":"))
    } else {
        implementation("dev.silenium.compose.gl:compose-gl:${composeGlVersion}")
    }
    implementation(libs.slf4j.api)
    runtimeOnly("ch.qos.logback:logback-classic:1.5.32")
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        mainClass = "dev.silenium.compose.gl.examples.skia_gl.MainKt"
    }
}
