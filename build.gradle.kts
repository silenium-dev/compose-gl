import dev.silenium.gradle.conventions.publishing
import dev.silenium.gradle.conventions.BaseExtension

plugins {
    dev.silenium.gradle.conventions.jvm
}

group = "dev.silenium.compose.gl"

allprojects {
    afterEvaluate {
        extensions.findByName("conventions")
            ?.let { it as BaseExtension }
            ?.apply {
                publishing {
                    pomSpec.set {
                        name = project.name
                        description = "KMP Library to render OpenGL content into a Compose scene"
                        url = "https://github.com/silenium-dev/compose-gl"
                        inceptionYear = "2024"
                        licenses {
                            license {
                                name = "GPL-3.0-or-later"
                                url = "https://spdx.org/licenses/GPL-3.0-or-later.html"
                            }
                        }
                        developers {
                            developer {
                                id = "silenium-dev"
                                email = "support@silenium-dev.net"
                            }
                        }
                        scm {
                            connection = "scm:git:git://github.com/silenium-dev/compose-gl.git"
                            developerConnection = "scm:git:ssh://github.com/silenium-dev/compose-gl.git"
                            url = "https://github.com/silenium-dev/compose-gl"
                        }
                    }
                }
            }
    }
}
