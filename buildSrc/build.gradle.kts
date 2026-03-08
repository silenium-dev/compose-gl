plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    maven("https://nexus.silenium.dev/repository/maven-releases") {
        name = "nexus"
    }
}

dependencies {
    api(libs.jni.utils)
}
