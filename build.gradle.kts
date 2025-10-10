import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.net.URLClassLoader
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.bytebuddy)
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

allprojects {
    apply<MavenPublishPlugin>()
    apply<BasePlugin>()

    this.group = "dev.silenium.compose.gl"
    this.version = findProperty("deploy.version") as String? ?: "0.0.0-SNAPSHOT"

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

val deployNative = (findProperty("deploy.native") as String?)?.toBoolean() ?: true
val deployKotlin = (findProperty("deploy.kotlin") as String?)?.toBoolean() ?: true

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
    libs.bundles.lwjgl.get().forEach {
        api(it)
        lwjglNatives.forEach { native ->
            runtimeOnly(variantOf(provider { it }) { classifier(native) })
        }
    }

    implementation(libs.bundles.kotlinx.coroutines)
    implementation("net.java.dev.jna:jna")
    api(libs.bundles.skiko)

    testImplementation(compose.desktop.currentOs)
    testImplementation(libs.logback.classic)
    testImplementation("me.saket.telephoto:zoomable:0.14.0")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    jvmToolchain(11)
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_2_0
        jvmTarget = JvmTarget.JVM_11
    }
}

val skiaVersion = configurations.compileClasspath.map {
    it.filter { it.name.matches(Regex("skiko-awt-\\d.+.jar")) }.singleFile
}.get().let {
    val loader = URLClassLoader(arrayOf(it.toURI().toURL()))
    val clazz = loader.loadClass("org.jetbrains.skiko.Version").kotlin
    val getSkikoVersion = clazz.functions.single { it.name == "getSkiko" }
    val getSkiaVersion = clazz.functions.single { it.name == "getSkia" }
    val constructor = clazz.constructors.single()
    constructor.isAccessible = true
    val instance = constructor.call()
    val skikoVersion = getSkikoVersion.call(instance)
    val skiaVersion = getSkiaVersion.call(instance)
    println("skiko version: $skikoVersion")
    println("skia version: $skiaVersion")
    rootProject.ext.set("skia.version", skiaVersion)
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

publishing {
    publications {
        if (deployKotlin) {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}
