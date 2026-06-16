plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
    maven("https://nexus.silenium.dev/repository/maven-releases/")
    maven("https://nexus.silenium.dev/repository/maven-snapshots/")
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
    configurations.implementation(libs.plugins.conventions.jvm)
    configurations.implementation(libs.plugins.conventions.kmp)
    configurations.implementation(libs.plugins.conventions.android.library)
    configurations.implementation(libs.plugins.conventions.android.application)
    implementation(gradleApi())
    implementation(gradleKotlinDsl())
    implementation(files(libs.javaClass.protectionDomain.codeSource.location.file))
}
