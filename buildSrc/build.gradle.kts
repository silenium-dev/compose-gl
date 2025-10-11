plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    maven("https://reposilite.silenium.dev/releases") {
        name = "silenium-releases"
    }
}

dependencies {
    api(libs.jni.utils)
}
