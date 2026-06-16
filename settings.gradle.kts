pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://nexus.silenium.dev/repository/maven-releases/")
        maven("https://nexus.silenium.dev/repository/maven-snapshots/")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "compose-gl"

val deployEnabled = if (extra.has("deploy.enabled")) {
    extra.get("deploy.enabled").toString().toBoolean()
} else false
include(":compose-gl", ":compose-gl:natives:desktop", ":compose-gl:natives:android")
if (!deployEnabled) {
    include(":examples", ":examples:skia-gl", ":examples:android-app")
}
