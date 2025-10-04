pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "compose-gl"

val deployNative = if (extra.has("deploy.native")) {
    extra.get("deploy.native")?.toString()?.toBoolean() ?: true
} else true
if (deployNative) {
    include(":native")
}
