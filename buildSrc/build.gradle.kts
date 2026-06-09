plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
    maven("https://nexus.silenium.dev/repository/maven-releases") {
        name = "nexus"
    }
}

dependencies {
    operator fun NamedDomainObjectProvider<Configuration>.invoke(plugin: Provider<PluginDependency>): ExternalModuleDependency {
        val plugin = plugin.get()
        return this("${plugin.pluginId}:${plugin.pluginId}.gradle.plugin") {
            version {
                plugin.version.preferredVersion.takeIf { it.isNotBlank() }?.let {
                    prefer(it)
                }
                plugin.version.requiredVersion.takeIf { it.isNotBlank() }?.let {
                    require(it)
                }
                plugin.version.strictVersion.takeIf { it.isNotBlank() }?.let {
                    strictly(it)
                }
                plugin.version.rejectedVersions.takeIf { it.isNotEmpty() }?.let {
                    reject(*it.toTypedArray())
                }
                branch = plugin.version.branch
            }
        }
    }
    configurations.implementation(plugin = libs.plugins.kotlin.multiplatform)
    configurations.implementation(plugin = libs.plugins.android.kotlin)
    configurations.implementation(plugin = libs.plugins.nixNatives)
    configurations.implementation(plugin = libs.plugins.compose)
    configurations.implementation(plugin = libs.plugins.kotlin.compose)
    configurations.implementation(plugin = libs.plugins.android.application)
    configurations.implementation(plugin = libs.plugins.kotlin.jvm)
    configurations.implementation(plugin = libs.plugins.idea.ext)
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(files(libs.javaClass.protectionDomain.codeSource.location.file))
}
