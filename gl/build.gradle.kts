import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.android
import dev.silenium.gradle.conventions.compileSdk
import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.publishing

plugins {
    dev.silenium.gradle.conventions.kmp
}

group = "dev.silenium.libs.gl"

val lwjglNatives = arrayOf("natives-linux", "natives-windows")
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.slf4j.api)
                implementation(libs.compose.ui.unit)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.slf4j)
                api(project(":gl:natives:android"))
            }
        }

        jvmMain {
            dependencies {
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
        namespace = "dev.silenium.libs.gl"
    }
    publishing {
        enabled = true
    }
}
