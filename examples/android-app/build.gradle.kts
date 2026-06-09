import com.android.build.gradle.internal.tasks.MergeNativeLibsTask
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

group = "dev.silenium.compose.gl.examples"

dependencies {
    implementation(project(":lib")) {
        exclude(group = "org.jetbrains.skiko")
    }
    implementation(libs.skiko)
    implementation(libs.slf4j.android)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.foundation:foundation:1.11.2")
    implementation("androidx.compose.runtime:runtime:1.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")
}

android {
    namespace = "dev.silenium.compose.gl.examples"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 26
        targetSdk = 37
    }
    packaging.resources.pickFirsts += "META-INF/*"
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
