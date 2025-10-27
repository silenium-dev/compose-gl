plugins {
    kotlin("jvm") version "2.2.20" apply false
    kotlin("plugin.compose") version "2.2.20" apply false
    id("org.jetbrains.compose") version "1.10.0-alpha03" apply false
}

group = "dev.silenium.compose.gl.sample"
version = "1.0-SNAPSHOT"

subprojects {
    repositories {
        mavenCentral()
        google()

        maven("https://repo.silenium.dev/releases") {
            name = "silenium-dev-releases"
        }
        maven("https://repo.silenium.dev/snapshots") {
            name = "silenium-dev-snapshots"
        }
    }
}
