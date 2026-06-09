import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.OutputDirectory
import java.io.File

abstract class AgpCopyCompat : Copy() {
    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    override fun getDestinationDir(): File {
        return destination.get().asFile
    }

    override fun copy() {
        destinationDir = destination.get().asFile
        return super.copy()
    }
}
