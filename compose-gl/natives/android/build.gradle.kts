import dev.silenium.build.ProjectConfig
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import dev.silenium.gradle.conventions.*

plugins {
    dev.silenium.gradle.conventions.android.library
    `maven-publish`
}

group = "dev.silenium.compose.gl.natives"


conventions {
    android {
        compileSdk {
            version = release(ProjectConfig.COMPILE_SDK)
        }
        namespace = "dev.silenium.compose.gl.natives.android"
        jvmTarget = ProjectConfig.ANDROID_JVM_TARGET
        cmakeVersion = ProjectConfig.CMAKE_VERSION
        ndkVersion = ProjectConfig.NDK_VERSION
    }
    publishing {
        enabled = true
    }
}

android {
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["default"])
            }
        }
    }

    configurations.forEach {
        if (it.name.startsWith("release") &&
            it.isCanBeConsumed && !it.isCanBeResolved &&
            (it.name.endsWith("RuntimeElements") || it.name.endsWith("ApiElements"))
        ) {
            it.attributes {
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(
                    TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                    objects.named(TargetJvmEnvironment.ANDROID),
                )
            }
        }
    }
}
