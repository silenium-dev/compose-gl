pluginManagement {
    repositories {
        maven("https://nexus.silenium.dev/repository/maven-releases") {
            name = "nexus"
        }
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "compose-gl"

val deployEnabled = if (extra.has("deploy.enabled")) {
    extra.get("deploy.enabled").toString().toBoolean()
} else false
include(":natives")
if (!deployEnabled) {
    include(":examples", ":examples:skia-gl")
}
