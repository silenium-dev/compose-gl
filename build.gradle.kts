import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

group = "dev.silenium.compose"
version = "0.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

val natives by configurations.creating

val lwjglVersion = "3.3.3"
val lwjglNatives = "natives-linux"

dependencies {
    implementation(kotlin("reflect"))
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    natives(project(":native", configuration = "main"))

    implementation(platform(libs.lwjgl.bom))
    implementation(libs.lwjgl.egl)
    libs.bundles.lwjgl.natives.get().forEach {
        implementation(it)
        runtimeOnly(variantOf(provider { it }) { classifier(lwjglNatives) })
    }

    implementation(libs.bundles.kotlinx.coroutines)
    implementation("org.jetbrains.skiko:skiko-awt:0.0.0-SNAPSHOT") {
        version {
            strictly("0.0.0-SNAPSHOT")
        }
    }
    implementation("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.0.0-SNAPSHOT") {
        version {
            strictly("0.0.0-SNAPSHOT")
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "compose-gl"
            packageVersion = "1.0.0"
        }
    }
}

tasks {
    processResources {
        dependsOn(":native:build")
        from(natives) {
            into("natives")
        }
    }

    compileKotlin {
        dependsOn(":native:build")
    }

    shadowJar {
        dependsOn(":native:build")
        from(natives) {
            into("natives")
        }

        manifest {
            attributes["Main-Class"] = "MainKt"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
