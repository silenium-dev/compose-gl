import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    `maven-publish`
}

repositories {
    maven("https://reposilite.silenium.dev/releases") {
        name = "reposilite"
    }
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

val deployNative = (findProperty("deploy.native") as String?)?.toBoolean() ?: true
val deployKotlin = (findProperty("deploy.kotlin") as String?)?.toBoolean() ?: true

val lwjglVersion = "3.3.4"
val lwjglNatives = arrayOf("natives-linux", "natives-windows")

dependencies {
    implementation(compose.desktop.common)
    implementation(libs.jni.utils)
    implementation(libs.slf4j.api)
    implementation(kotlin("reflect"))
    if (deployNative) {
        implementation(project(":native", configuration = "main"))
    }

    api(platform(libs.lwjgl.bom))
    api(libs.lwjgl.egl)
    libs.bundles.lwjgl.natives.get().forEach {
        api(it)
        lwjglNatives.forEach { native ->
            runtimeOnly(variantOf(provider { it }) { classifier(native) })
        }
    }

    implementation(libs.bundles.kotlinx.coroutines)
//    api(libs.bundles.skiko) {
//        version {
//            strictly(libs.skiko.awt.runtime.linux.x64.get().version!!)
//        }
//    }

    testImplementation(compose.desktop.currentOs)
    testImplementation(libs.logback.classic)
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

java {
    withSourcesJar()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

allprojects {
    apply<MavenPublishPlugin>()
    apply<BasePlugin>()

    group = "dev.silenium.compose.gl"
    version = findProperty("deploy.version") as String? ?: "0.0.0-SNAPSHOT"

    publishing {
        repositories {
            val url = System.getenv("MAVEN_REPO_URL") ?: return@repositories
            maven(url) {
                name = "reposilite"
                credentials {
                    username = System.getenv("MAVEN_REPO_USERNAME") ?: ""
                    password = System.getenv("MAVEN_REPO_PASSWORD") ?: ""
                }
            }
        }
    }
}

publishing {
    publications {
        if (deployKotlin) {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}
