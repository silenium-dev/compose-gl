import dev.silenium.libs.jni.Platform
import org.gradle.api.Project

fun Project.nativeArtifactId(platform: Platform): String {
    return "${rootProject.name}-natives-${platform.full}"
}

val Project.allNativesArtifactId get() = rootProject.name + "-natives-all"
