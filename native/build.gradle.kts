import org.jetbrains.kotlin.incremental.createDirectory

plugins {
    base
}

configurations.create("main")

tasks {
    register<Exec>("generateMakefile") {
        workingDir = layout.buildDirectory.dir("cmake").get().asFile.apply { createDirectory() }
        commandLine("cmake", "-GNinja", layout.projectDirectory.asFile.absolutePath)

        inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
        outputs.dir(workingDir)
    }
    register<Exec>("compileNative") {
        workingDir = layout.buildDirectory.dir("cmake").get().asFile
        commandLine("ninja")
        dependsOn("generateMakefile")

        inputs.file(layout.buildDirectory.file("cmake/build.ninja"))
        inputs.dir(layout.projectDirectory.dir("src"))
        inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
        outputs.files(layout.buildDirectory.file("cmake/libcompose-gl.so"))
    }

    build {
        dependsOn("compileNative")
    }
}

artifacts {
    tasks["compileNative"].outputs.files.onEach {
        add("main", it)
    }
}
