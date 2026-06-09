import com.android.build.api.dsl.LibraryBuildType
import org.gradle.tooling.model.build.JavaEnvironment
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    com.android.library
}

group = "dev.silenium.compose.gl.natives"

androidTestDependencies()

android {
    namespace = "dev.silenium.compose.gl.natives.android"
    commonConfig()

    externalNativeBuild {
        cmake {
            version = ProjectConfig.CMAKE_VERSION
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
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
