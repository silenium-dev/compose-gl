import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.dsl.ManagedVirtualDevice
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

object ProjectConfig {
    const val COMPILE_SDK = 37
    const val MIN_SDK = 26
    const val NDK_VERSION = "29.0.14206865"
    const val CMAKE_VERSION = "4.1.2"
    val JVM_TARGET = JvmTarget.JVM_11
    val ANDROID_JVM_TARGET = JvmTarget.JVM_11
}

fun CommonExtension.commonConfig() {
    compileSdk {
        version = release(ProjectConfig.COMPILE_SDK)
    }
    ndkVersion = ProjectConfig.NDK_VERSION
    buildToolsVersion = ProjectConfig.COMPILE_SDK.toString()
    externalNativeBuild.cmake.version = ProjectConfig.CMAKE_VERSION

    defaultConfig.minSdk = ProjectConfig.MIN_SDK
    defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    defaultConfig.testInstrumentationRunnerArguments["notPackage"] = "com.v7878"

    compileOptions.targetCompatibility =
        JavaVersion.toVersion(ProjectConfig.ANDROID_JVM_TARGET.target)
    compileOptions.sourceCompatibility =
        JavaVersion.toVersion(ProjectConfig.ANDROID_JVM_TARGET.target)

    packaging.resources.pickFirsts += "META-INF/*"

    val pixel =
        testOptions.managedDevices.allDevices.create<ManagedVirtualDevice>("pixel10X86_64") {
            device = "Pixel 10 Pro"
            sdkVersion = 30
            testedAbi = "x86_64"
            require64Bit = true
            systemImageSource = "aosp-atd"
        }
    testOptions.managedDevices.groups.create("utp") {
        targetDevices.add(pixel)
    }
}

fun KotlinMultiplatformAndroidLibraryTarget.commonConfig() {
    compileSdk {
        version = release(ProjectConfig.COMPILE_SDK)
    }
    buildToolsVersion = ProjectConfig.COMPILE_SDK.toString()
    minSdk = ProjectConfig.MIN_SDK
    compilerOptions {
        jvmTarget.set(ProjectConfig.ANDROID_JVM_TARGET)
    }

    withHostTest {
        enableCoverage = true
    }
    withDeviceTest {
        enableCoverage = true
        instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        instrumentationRunnerArguments["notPackage"] = "com.v7878"
    }
    packaging.resources.pickFirsts += "META-INF/*"
}

fun Project.androidTestDependencies() {
    val androidTestImplementation =
        configurations.getByName<Configuration>("androidTestImplementation")
    dependencies {
        androidTestImplementation(libs.slf4j.android)
        androidTestImplementation(libs.bundles.androidx.test)
        androidTestImplementation(libs.bundles.tests)
        androidTestImplementation(libs.kotest.runner.junit4)
    }
}
