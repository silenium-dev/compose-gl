import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.*

plugins {
    dev.silenium.gradle.conventions.jvm
    org.jetbrains.kotlin.plugin.compose
    org.jetbrains.compose
}

group = "dev.silenium.compose.gl.examples"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project(":compose-gl"))
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}

conventions {
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }
}

compose.desktop {
    application {
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        mainClass = "dev.silenium.compose.gl.examples.skia_gl.MainKt"
    }
}
