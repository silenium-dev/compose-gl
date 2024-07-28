import dev.silenium.libs.jni.NativeLoader
import dev.silenium.libs.jni.NativePlatform
import dev.silenium.libs.jni.Platform
import org.jetbrains.kotlin.incremental.createDirectory

buildscript {
    repositories {
        maven("https://reposilite.silenium.dev/releases") {
            name = "silenium-releases"
        }
    }
    dependencies {
        classpath(libs.jni.utils)
    }
}

plugins {
    base
    `maven-publish`
}

val main: Configuration by configurations.creating

val libName = rootProject.name
val deployNative = (findProperty("deploy.native") as String?)?.toBoolean() ?: true

val platformString = findProperty("deploy.platform")?.toString()
val platform = platformString?.let(Platform::invoke) ?: NativePlatform.platform()

val cmakeExe = findProperty("cmake.executable") as? String ?: "cmake"
val generateMakefile = tasks.register<Exec>("generateMakefile") {
    workingDir = layout.buildDirectory.dir("cmake").get().asFile.apply { createDirectory() }
    val additionalFlags = mutableListOf(
        "-DJAVA_HOME=${System.getProperty("java.home")}",
        "-DPROJECT_NAME=${libName}",
    )
    commandLine(
        cmakeExe,
        *additionalFlags.toTypedArray(),
        layout.projectDirectory.asFile.absolutePath,
    )

    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    inputs.properties(
        "JAVA_HOME" to System.getProperty("java.home"),
        "PROJECT_NAME" to libName,
    )
    outputs.dir(workingDir)
    standardOutput = System.out
}

val compileNative = tasks.register<Exec>("compileNative") {
    workingDir = layout.buildDirectory.dir("cmake").get().asFile
    commandLine(cmakeExe, "--build", ".")
    dependsOn(generateMakefile)

    standardOutput = System.out
    val fileNameTemplate = NativeLoader.fileNameTemplate(platform)
    when (platform.os) {
        Platform.OS.WINDOWS -> {
            outputs.files(layout.buildDirectory.file("cmake/Debug/${fileNameTemplate.format(libName)}"))
        }

        Platform.OS.LINUX, Platform.OS.DARWIN -> {
            outputs.files(layout.buildDirectory.file("cmake/${fileNameTemplate.format(libName)}"))
        }
    }
    inputs.file(layout.buildDirectory.file("cmake/CMakeCache.txt"))
    inputs.dir(layout.projectDirectory.dir("src"))
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    outputs.cacheIf { true }
}

val jar = tasks.register<Jar>("nativeJar") {
    dependsOn(compileNative)
    // Required for configuration cache
    val libName = rootProject.name
    val platformString = findProperty("deploy.platform")?.toString()
    val platform = platformString?.let(Platform::invoke) ?: NativePlatform.platform()

    from(compileNative.get().outputs.files) {
        rename {
            NativeLoader.libPath(libName, platform = platform)
        }
    }
}

artifacts {
    add(main.name, jar)
}

publishing {
    publications {
        create<MavenPublication>("natives${platform.capitalized}") {
            artifact(jar)
            artifactId = "$libName-natives-$platform"
        }
    }
}
