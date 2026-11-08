import dev.silenium.build.PrepareSkiaVersionsJson
import dev.silenium.build.ProjectConfig
import dev.silenium.build.SkiaArch.arm64
import dev.silenium.build.SkiaArch.x64
import dev.silenium.build.SkiaBuildType.Release
import dev.silenium.build.SkiaOs.linux
import dev.silenium.build.SkiaOs.windows
import dev.silenium.build.SkiaTarget
import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.publishing

plugins {
    dev.silenium.gradle.conventions.jvm
    dev.silenium.libs.jni.`nix-natives`
}

group = "dev.silenium.compose.gl.natives"

val skiko = configurations.register("skiko") {
    isTransitive = false
}

dependencies {
    skiko(libs.skiko)
}

val prepareSkiaVersionsJson = tasks.register<PrepareSkiaVersionsJson>("prepareSkiaVersionsJson") {
    description = "resolve skia assets as nix build inputs"
    skikoConfiguration.from(skiko)
    skiaTargets = setOf(
        SkiaTarget(linux, Release, x64),
        SkiaTarget(linux, Release, arm64),
        SkiaTarget(windows, Release, x64),
        SkiaTarget(windows, Release, arm64),
    )
    versionsJson = file("nix/versions.json")
}

nixNatives {
    libName = "compose-gl"
    libVersion = version.toString()
    nixFlake = file("flake.nix")
    sourceFiles.from("src", "meson.build", "subprojects.tpl", "nix/skia-config.nix", prepareSkiaVersionsJson)
    showLogs = providers.environmentVariable("CI").orElse("false").map { it != "false" }
}

conventions {
    publishing {
        enabled = true
    }
    jvm {
        jvmTarget = ProjectConfig.JVM_TARGET
    }
}
