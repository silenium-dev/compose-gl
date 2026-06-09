plugins {
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
        }
    }
}
