pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "compose-gl"

val deployNative = if (extra.has("deploy.native")) {
    extra.get("deploy.native")?.toString()?.toBoolean() ?: true
} else true
if (deployNative) {
    include(":native")
}
