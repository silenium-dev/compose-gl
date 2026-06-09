plugins {
    org.jetbrains.kotlin.jvm
    org.jetbrains.kotlin.plugin.compose
    org.jetbrains.compose
}

group = "dev.silenium.compose.gl.examples"
val composeGlVersion = "0.11.0-rc.1"

val useParent: Boolean =
    project.ext.properties.getOrDefault("examples.use-parent", "true").toString().toBoolean()

dependencies {
    implementation(compose.desktop.currentOs)
    if (useParent) {
        implementation(project(":lib"))
    } else {
        implementation("dev.silenium.compose.gl:compose-gl:${composeGlVersion}")
    }
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}

kotlin {
    jvmToolchain(21)
}

compose.desktop {
    application {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        mainClass = "dev.silenium.compose.gl.examples.skia_gl.MainKt"
    }
}
