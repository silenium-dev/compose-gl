import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.accessors.dm.LibrariesForLibsInPluginsBlock
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.PluginDependenciesSpecScope
import org.gradle.kotlin.dsl.the

val Project.libs get() = the<LibrariesForLibs>()

//val PluginDependenciesSpecScope.libs: LibrariesForLibsInPluginsBlock
//    get() = PluginCatalogResolver.resolve(this, "libs") as LibrariesForLibsInPluginsBlock

interface ComposeGlBaseExtension {
    val publish: Property<Boolean>
}
