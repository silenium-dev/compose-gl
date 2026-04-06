import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.TaskTriggersConfig
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URLClassLoader
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.idea.ext)
    `maven-publish`
}

val deployEnabled = (findProperty("deploy.enabled") as String?)?.toBoolean() ?: false

allprojects {
    apply<MavenPublishPlugin>()
    apply<BasePlugin>()

    group = "dev.silenium.compose.gl"
    val gitVersionProvider = providers.gradleProperty("ci").flatMap {
        if (it.toBoolean()) {
            providers.exec {
                commandLine("git", "describe", "--tags")
                workingDir = layout.projectDirectory.asFile
            }.standardOutput.asText.map(String::trim)
        } else null
    }
    version = providers
        .gradleProperty("deploy.version")
        .orElse(gitVersionProvider)
        .orElse("0.0.0-SNAPSHOT")
        .get()

    repositories {
        maven("https://nexus.silenium.dev/repository/maven-releases") {
            name = "nexus"
        }
        mavenCentral()
        google()
    }

    publishing {
        repositories {
            if (deployEnabled) {
                val url = findProperty("deploy.repo-url") as? String ?: error("No deploy.repo-url specified")
                maven(url) {
                    name = "nexus"
                    credentials {
                        username = findProperty("deploy.username") as? String ?: ""
                        password = findProperty("deploy.password") as? String ?: ""
                    }
                }
            }
        }
    }
}

val lwjglNatives = arrayOf("natives-linux", "natives-windows")

dependencies {
    implementation(libs.compose.desktop)
    implementation(libs.jni.utils)
    implementation(libs.slf4j.api)
    implementation(kotlin("reflect"))
    implementation(project(":natives"))

    api(platform(libs.lwjgl.bom))
    libs.bundles.lwjgl.get().forEach {
        api(it)
        lwjglNatives.forEach { native ->
            runtimeOnly(variantOf(provider { it }) { classifier(native) })
        }
    }

    implementation(libs.bundles.kotlinx.coroutines)
    api(libs.bundles.skiko)

    testImplementation(compose.desktop.currentOs)
    testImplementation(libs.logback.classic)
    testImplementation("me.saket.telephoto:zoomable:0.19.0")
}


configurations.compileClasspath.map {
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
    rootProject.ext.set("skiko.version", skikoVersion)
    rootProject.ext.set("skia.version", skiaVersion)
}

val templateSrc = layout.projectDirectory.dir("src/main/templates")
val templateDst = layout.buildDirectory.dir("generated/templates")
val templateProps = mapOf(
    "LIBRARY_NAME" to rootProject.name,
)
tasks {
    test {
        useJUnitPlatform()
    }

    val generateTemplates = register<Copy>("generateTemplates") {
        from(templateSrc)
        into(templateDst)
        expand(templateProps)

        inputs.dir(templateSrc)
        inputs.properties(templateProps)
        outputs.dir(templateDst)
    }

    withType<Jar> {
        dependsOn(generateTemplates)
    }

    compileKotlin {
        dependsOn("generateTemplates")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

sourceSets.main {
    kotlin {
        srcDir(templateDst)
    }
}

java {
    sourceCompatibility = kotlin.compilerOptions.jvmTarget.map { JavaVersion.toVersion(it.target) }.get()
    targetCompatibility = sourceCompatibility

    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("main") {
            from(components["java"])
        }
    }
}

rootProject.idea.project {
    this as ExtensionAware
    configure<ProjectSettings> {
        this as ExtensionAware
        configure<TaskTriggersConfig> {
            afterSync("generateTemplates")
        }
    }
}
