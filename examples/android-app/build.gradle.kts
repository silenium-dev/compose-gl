import dev.silenium.build.PrepareAndroidSkikoNatives
import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.android
import dev.silenium.gradle.conventions.bundleNatives
import dev.silenium.gradle.conventions.compileSdk

plugins {
    dev.silenium.gradle.conventions.android.application
    org.jetbrains.kotlin.plugin.compose
}

group = "dev.silenium.compose.gl.examples"

repositories {
    maven("https://packages.jetbrains.team/maven/p/cmp/dev")
}

val skikoJniClasspath by configurations.registering {
    isCanBeResolved = true
    isCanBeConsumed = false
}

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

    skikoJniClasspath(libs.skiko.android.runtime.arm64)
    skikoJniClasspath(libs.skiko.android.runtime.x64)
}

val prepareJniLibs by tasks.registering(PrepareAndroidSkikoNatives::class) {
    skikoNatives.from(skikoJniClasspath)
    destinationDirectory = layout.buildDirectory.dir("generated")
    archiveFileName = "skiko-natives.zip"
}

conventions {
    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        namespace = "dev.silenium.compose.gl.examples"
        minSdk = ProjectConfig.MIN_SDK
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET

        bundleNatives {
            configuration.from(prepareJniLibs)
            libraries.addAll(
                "libskiko-android-arm64.so",
                "libskiko-android-x64.so",
            )
        }
    }
}
