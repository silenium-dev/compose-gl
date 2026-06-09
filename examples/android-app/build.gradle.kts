import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    com.android.application
    org.jetbrains.kotlin.plugin.compose
}

group = "dev.silenium.compose.gl.examples"

dependencies {
    implementation(project(":compose-gl")) {
        exclude(group = "org.jetbrains.skiko")
    }
    implementation(libs.skiko)
    implementation(libs.slf4j.android)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.foundation:foundation:1.11.2")
    implementation("androidx.compose.runtime:runtime:1.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")

    androidTestDependencies()
}

android {
    commonConfig()
    namespace = "dev.silenium.compose.gl.examples"
}

// 1. Create a dedicated configuration just to resolve the jars
val skikoJniClasspath by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

// 2. Add the runtime dependencies specifically to this configuration
dependencies {
    skikoJniClasspath(libs.skiko.android.runtime.arm64)
    skikoJniClasspath(libs.skiko.android.runtime.x64)
}

// 3. Register a copy task that maps the flat .so files into standard ABI folders
val archiveOps = serviceOf<ArchiveOperations>()
val extractSkikoJni = tasks.register<AgpCopyCompat>("extractSkikoJni") {
    from(skikoJniClasspath.map { zipTree(it) }) {
        include("*.so")
        eachFile {
            if (name.contains("arm64")) {
                path = "arm64-v8a/$name"
            } else if (name.contains("x64")) {
                path = "x86_64/$name"
            }
        }
        includeEmptyDirs = false
    }
    destination = layout.buildDirectory.dir("skiko_jni")
}

androidComponents.onVariants {
    it.sources.jniLibs?.addGeneratedSourceDirectory(extractSkikoJni, AgpCopyCompat::destination)
}
