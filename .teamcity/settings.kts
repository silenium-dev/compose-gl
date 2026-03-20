import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.projectFeatures.UntrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.projectFeatures.githubConnection
import jetbrains.buildServer.configs.kotlin.projectFeatures.untrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.triggers.vcs

version = "2025.11"

project {
    buildType(BuildSnapshot)
    buildType(BuildRelease)

    features {
        githubConnection {
            id = "PROJECT_EXT_10"
            displayName = "GitHub.com"
            clientId = "Ov23liT97vPVEYmQSgyV"
            clientSecret = "credentialsJSON:caa167f1-c169-4bab-add1-445d1770fe1d"
        }

        untrustedBuildsSettings {
            enableLog = true
            manualRunsApproved = true
            defaultAction = UntrustedBuildsSettings.DefaultAction.APPROVE
            approvalRules = """
                (groups:MAINTAINERS):1
            """.trimIndent()
        }
    }
}

object BuildRelease : BuildType({
    name = "Build Release"

    vcs {
        root(DslContext.settingsRoot)
        branchFilter = """
            |+:*
        """.trimMargin()
    }

    features {
        perfmon {
        }

        vcsLabeling {
            vcsRootId = "${DslContext.settingsRoot.id}"
            labelingPattern = "%release.version%"
            successfulOnly = true
            branchFilter = """
                |+:*
            """.trimMargin()
        }
    }

    requirements {
        exists("system.nix.store")
    }

    params {
        text(
            "release.version",
            "",
            description = "Version to release",
            display = ParameterDisplay.PROMPT,
            allowEmpty = false
        )
        text(
            "deploy.repo-url",
            "https://nexus.silenium.dev/repository/maven-releases",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        text(
            "deploy.username",
            "teamcity-ci",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        password(
            "deploy.password",
            "credentialsJSON:149ec97d-3f03-4588-b740-38f933c0d1e2",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
    }

    steps {
        gradle {
            tasks = """
                |build
                |publish
            """.trimMargin().replace("\n", " ")
            gradleParams = """
                |-Pdeploy.version=%release.version%
                |-Pdeploy.enabled=true
                |-Pdeploy.repo-url=%deploy.repo-url%
                |-Pdeploy.username=%deploy.username%
                |-Pdeploy.password=%deploy.password%
                |--scan
                |--info
            """.trimMargin().replace("\n", " ")
        }
    }
})

object BuildSnapshot : BuildType({
    name = "Build Snapshot"

    vcs {
        root(DslContext.settingsRoot)
    }

    triggers {
        vcs {
            branchFilter = """
                |+:*
            """.trimMargin()
        }
    }

    features {
        perfmon {
        }

        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = vcsRoot()
            }
        }
    }

    requirements {
        exists("system.nix.store")
    }

    params {
        text(
            "deploy.repo-url",
            "https://nexus.silenium.dev/repository/maven-snapshots",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        text(
            "deploy.username",
            "teamcity-ci",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        password(
            "deploy.password",
            "credentialsJSON:149ec97d-3f03-4588-b740-38f933c0d1e2",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
    }

    steps {
        gradle {
            tasks = """
                |build
                |publish
            """.trimMargin().replace("\n", " ")
            gradleParams = """
                |-Pci=true
                |-Pdeploy.enabled=true
                |-Pdeploy.repo-url=%deploy.repo-url%
                |-Pdeploy.username=%deploy.username%
                |-Pdeploy.password=%deploy.password%
                |--scan
                |--info
            """.trimMargin().replace("\n", " ")
        }
    }
})

object BuildPR : BuildType({
    name = "Build Pull Request"

    vcs {
        root(DslContext.settingsRoot)
    }

    features {
        perfmon {
        }

        pullRequests {
            vcsRootExtId = "${DslContext.settingsRoot.id}"
            provider = github {
                authType = vcsRoot()
                ignoreDrafts = true
            }
        }

        commitStatusPublisher {
            publisher = github {
                githubUrl = "https://api.github.com"
                authType = vcsRoot()
            }
        }
    }

    requirements {
        exists("system.nix.store")
    }

    steps {
        gradle {
            tasks = """
                |build
                |publish
            """.trimMargin().replace("\n", " ")
            gradleParams = """
                |-Pdeploy.enabled=false
                |--scan
                |--info
            """.trimMargin().replace("\n", " ")
        }
    }
})
