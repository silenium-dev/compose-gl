import dev.silenium.libs.jni.Platform
import dev.silenium.libs.jni.Platform.Arch
import dev.silenium.libs.jni.Platform.OS

plugins {
    `java-platform`
}

val supportedPlatforms = listOf(
    Platform(OS.LINUX, Arch.X86_64),
    Platform(OS.LINUX, Arch.ARM64),
    Platform(OS.WINDOWS, Arch.X86_64),
    Platform(OS.WINDOWS, Arch.ARM64),
)
val libName = rootProject.name

javaPlatform {
    allowDependencies()
}

dependencies {
    api(libs.jni.utils)
    supportedPlatforms.forEach { platform ->
        api("${project.group}:${nativeArtifactId(platform)}:${project.version}")
    }
}

publishing {
    publications {
        create<MavenPublication>("nativesAll") {
            artifactId = allNativesArtifactId
            from(components["javaPlatform"])
        }
    }
}
