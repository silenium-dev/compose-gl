import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    `maven-publish`
}

group = "dev.silenium.compose"
version = findProperty("deploy.version") as String? ?: "0.0.0-SNAPSHOT"

repositories {
    maven("https://reposilite.silenium.dev/snapshots") {
        name = "reposilite"
    }
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
//    natives(project(":native", configuration = "main"))

    implementation(platform(libs.lwjgl.bom))
    implementation(libs.lwjgl.egl)
    libs.bundles.lwjgl.natives.get().forEach {
        implementation(it)
        runtimeOnly(variantOf(provider { it }) { classifier(lwjglNatives) })
    }

    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.skiko) {
        version {
            strictly(libs.bundles.skiko.get().first().version!!)
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
//    processResources {
//        dependsOn(":native:build")
//        from(natives) {
//            into("natives")
//        }
//    }

//    compileKotlin {
//        dependsOn(":native:build")
//    }

//    jar {
//        dependsOn(":native:build")
//        from(natives) {
//            into("natives")
//        }
//    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven("https://reposilite.silenium.dev/snapshots") {
            name = "reposilite"
            credentials {
                username = System.getenv("REPOSILITE_USERNAME") ?: project.findProperty("reposiliteUser") as String? ?: ""
                password = System.getenv("REPOSILITE_PASSWORD") ?: project.findProperty("reposilitePassword") as String? ?: ""
            }
        }
    }
}
