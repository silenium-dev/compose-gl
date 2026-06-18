import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.BuildStep.ExecutionMode
import jetbrains.buildServer.configs.kotlin.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.pullRequests
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.gitHubRelease
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.UntrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.projectFeatures.untrustedBuildsSettings
import jetbrains.buildServer.configs.kotlin.triggers.vcs

version = "2026.1"

project {
    buildType(BuildSnapshot)
    buildType(BuildRelease)

    features {
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
            "nexus.repo-url",
            "https://nexus.silenium.dev/repository/maven-releases",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        text(
            "nexus.username",
            "teamcity-ci",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        password(
            "nexus.password",
            "credentialsJSON:149ec97d-3f03-4588-b740-38f933c0d1e2",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )

        password(
            "maven-central.username",
            "credentialsJSON:252356db-9418-4ae9-8f06-0d16bc690805",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        password(
            "maven-central.password",
            "credentialsJSON:6375fac2-1d65-4f4e-bfea-09191d89f44d",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )

        password(
            "gpg.secret-key",
            "credentialsJSON:aa15cd06-be04-40d6-9569-a781b94f5d9c",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        password(
            "gpg.public-key",
            "credentialsJSON:03a34fba-18cb-4875-8996-135df4d49efd",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        password(
            "gpg.passphrase",
            "credentialsJSON:becb8d56-5e47-4e7f-847c-166dcaae1a34",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
    }

    steps {
        val gradleArgs = """
                |-Pdeploy.version=%release.version%
                |-Pdeploy.enabled=true
                |-Pnexus.enabled=true
                |-Pnexus.repo-url=%nexus.repo-url%
                |-Pnexus.username=%nexus.username%
                |-Pnexus.password=%nexus.password%
                |-Pmaven-central.enabled=true
                |-Pmaven-central.username=%maven-central.username%
                |-Pmaven-central.password=%maven-central.password%
                |-Pgpg.secret-key=%gpg.secret-key%
                |-Pgpg.public-key=%gpg.public-key%
                |-Pgpg.passphrase=%gpg.passphrase%
                |-Pgpg.enabled=true
                |--scan
                |--info
            """.trimMargin().replace("\n", " ")
        gradle {
            tasks = """
                |clean
                |build
                |publish
            """.trimMargin().replace("\n", " ")
            gradleParams = gradleArgs
        }
        gradle {
            tasks = """
                |jreleaserDeploy
            """.trimMargin().replace("\n", " ")
            gradleParams = gradleArgs
        }
        gitHubRelease {
            name = "Create GitHub Release"
            targetVcsRootId = "${DslContext.settingsRoot.id}"
            githubUrl = "https://api.github.com"
            tagName = "%release.version%"
            generateReleaseNotes = true
            draft = true
            authType = vcsRoot()
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
            "nexus.repo-url",
            "https://nexus.silenium.dev/repository/maven-snapshots",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        text(
            "nexus.username",
            "teamcity-ci",
            display = ParameterDisplay.HIDDEN,
            readOnly = true
        )
        password(
            "nexus.password",
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
                |-Pnexus.enabled=true
                |-Pnexus.repo-url=%nexus.repo-url%
                |-Pnexus.username=%nexus.username%
                |-Pnexus.password=%nexus.password%
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
