import dev.silenium.build.ProjectConfig
import dev.silenium.gradle.conventions.jvm
import dev.silenium.gradle.conventions.publishing

plugins {
    dev.silenium.gradle.conventions.jvm
    dev.silenium.libs.jni.`nix-natives`
}

group = "dev.silenium.compose.gl.natives"

nixNatives {
    libName = "compose-gl"
    libVersion = version.toString()
    nixFlake = file("flake.nix")
    sourceFiles.from("src", "meson.build", "subprojects.tpl")
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
