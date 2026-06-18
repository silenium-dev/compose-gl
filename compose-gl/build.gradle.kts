import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.android
import dev.silenium.gradle.conventions.compileSdk
import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.publishing

plugins {
    org.jetbrains.kotlin.plugin.compose
    dev.silenium.gradle.conventions.kmp
}

group = "dev.silenium.compose.gl"

val lwjglNatives = arrayOf("natives-linux", "natives-windows")
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.slf4j.api)
                implementation(libs.compose.runtime)
                implementation(libs.compose.ui.get().toString()) {
                    exclude("androidx.compose.runtime")
                }
            }
        }

        androidMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.slf4j)
                implementation(project(":compose-gl:natives:android"))
            }
        }

        jvmMain {
            dependencies {
                implementation(project(":compose-gl:natives:desktop"))
                implementation(libs.compose.foundation.get().toString()) {
                    exclude("androidx.compose.runtime")
                }
                implementation(libs.jni.utils)
                implementation(libs.kotlinx.coroutines.slf4j)
                api(dependencies.platform(libs.lwjgl.bom))
                api(libs.bundles.lwjgl)
                libs.bundles.lwjgl.get().forEach {
                    lwjglNatives.forEach { native ->
                        runtimeOnly(dependencies.variantOf(provider { it }) { classifier(native) })
                    }
                }
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.bundles.kotlinx.coroutines.jvm)
            }
        }
    }
}

conventions {
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }
    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        minSdk = ProjectConfig.MIN_SDK
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET
        namespace = "dev.silenium.compose.gl"
    }
    publishing {
        enabled = true
    }
}

tasks {
    named<JavaCompile>("compileJvmMainJava") {
        options.compilerArgs.addAll(listOf("--add-reads", "dev.silenium.compose.gl=ALL-UNNAMED"))
    }
}
