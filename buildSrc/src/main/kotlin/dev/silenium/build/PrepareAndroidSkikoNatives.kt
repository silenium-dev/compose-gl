package dev.silenium.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import javax.inject.Inject

abstract class PrepareAndroidSkikoNatives : Zip() {
    @get:InputFiles
    abstract val skikoNatives: ConfigurableFileCollection

    @get:Inject
    protected abstract val archiveOps: ArchiveOperations

    init {
        description = "Copies skiko libs into a android compatible layout"
        from(skikoNatives.elements.map { it.map(archiveOps::zipTree) }) {
            include("*.so")
            eachFile {
                if (name.contains("arm64")) {
                    path = "arm64-v8a/$name"
                } else if (name.contains("x64")) {
                    path = "x86_64/$name"
                }
            }
            includeEmptyDirs = false
        }
    }
}
