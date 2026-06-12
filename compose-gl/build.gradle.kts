import java.net.URLClassLoader
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

plugins {
    com.android.kotlin.multiplatform.library
    org.jetbrains.kotlin.multiplatform
    org.jetbrains.kotlin.plugin.compose
    org.jetbrains.compose
    `maven-publish`
}

val lwjglNatives = arrayOf("natives-linux", "natives-windows")
kotlin {
    jvmToolchain(ProjectConfig.JVM_TARGET.target.toInt())
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    jvm {
        compilerOptions {
            jvmTarget = ProjectConfig.JVM_TARGET
        }
    }
    android {
        namespace = "dev.silenium.compose.gl"
        commonConfig()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("reflect"))
                implementation(libs.bundles.kotlinx.coroutines)
                implementation(libs.slf4j.api)
                implementation(libs.bundles.compose.common)
            }
        }

        androidMain {
            dependencies {
                implementation(project(":compose-gl:natives:android"))
            }
        }

        jvmMain {
            dependencies {
                implementation(project(":compose-gl:natives:desktop"))
                implementation(libs.compose.desktop)
                implementation(libs.jni.utils)
                api(dependencies.platform(libs.lwjgl.bom))
                libs.bundles.lwjgl.get().forEach {
                    api(it)
                    lwjglNatives.forEach { native ->
                        runtimeOnly(dependencies.variantOf(provider { it }) { classifier(native) })
                    }
                }
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.logback.classic)
            }
        }
    }
}

val skikoVersionClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    skikoVersionClasspath(libs.compose.desktop)
}

skikoVersionClasspath.let {
    val file = it.filter { it.name.matches(Regex("skiko-awt-\\d.+.jar")) }.singleFile
    val loader = URLClassLoader(arrayOf(file.toURI().toURL()))
    val clazz = loader.loadClass("org.jetbrains.skiko.Version").kotlin
    val getSkikoVersion = clazz.functions.single { it.name == "getSkiko" }
    val getSkiaVersion = clazz.functions.single { it.name == "getSkia" }
    val constructor = clazz.constructors.single()
    constructor.isAccessible = true
    val instance = constructor.call()
    val skikoVersion = getSkikoVersion.call(instance)
    val skiaVersion = getSkiaVersion.call(instance)
    println("skiko version: $skikoVersion")
    println("skia version: $skiaVersion")
    rootProject.ext.set("skiko.version", skikoVersion)
    rootProject.ext.set("skia.version", skiaVersion)
}

tasks {
    withType<Test> {
        useJUnitPlatform()
    }

    named<JavaCompile>("compileJvmMainJava") {
        options.compilerArgs.addAll(listOf("--add-reads", "dev.silenium.compose.gl=ALL-UNNAMED"))
    }
}
