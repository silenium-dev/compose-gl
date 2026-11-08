package dev.silenium.build

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.net.URLClassLoader
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

enum class SkiaBuildType {
    Debug,
    Release,
}

enum class SkiaOs(val archs: Set<SkiaArch> = setOf(SkiaArch.x64, SkiaArch.arm64)) {
    android,
    ios,
    iosSim,
    linux,
    macos,
    tvos(setOf(SkiaArch.arm64)),
    tvosSim,
    wasm(setOf(SkiaArch.wasm)),
    windows;

    fun validate(arch: SkiaArch) = check(arch in archs) { "unsupported arch $arch for os $this (supported: $archs)" }
}

enum class NixArch {
    x86_64,
    aarch64,
}

enum class SkiaArch(val nix: NixArch? = null) {
    x64(NixArch.x86_64),
    arm64(NixArch.aarch64),
    wasm,
}

@Serializable(with = SkiaTargetSerializer::class)
data class SkiaTarget(val os: SkiaOs, val buildType: SkiaBuildType, val arch: SkiaArch): java.io.Serializable {
    init {
        os.validate(arch)
    }

    companion object {
        @JvmStatic
        val serialVersionUID: Long = -56454289740198428L
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = SkiaTarget::class)
object DefaultSkiaTargetSerializer

object SkiaTargetSerializer : KSerializer<SkiaTarget> {
    override val descriptor: SerialDescriptor = serialDescriptor<String>()

    override fun serialize(encoder: Encoder, value: SkiaTarget) {
        encoder.encodeString("${value.os}-${value.buildType}-${value.arch}")
    }

    override fun deserialize(decoder: Decoder): SkiaTarget {
        val value = decoder.decodeString()
        val components = value.split('-')
        if (components.size != 3) {
            throw SerializationException("Invalid input for skia target: ${value}")
        }
        return SkiaTarget(
            SkiaOs.valueOf(components[0]),
            SkiaBuildType.valueOf(components[1]),
            SkiaArch.valueOf(components[2]),
        )
    }
}

@Serializable
private data class GithubRelease(
    val id: Long,
    @SerialName("tag_name")
    val tagName: String,
    val assets: List<Asset>,
) {
    @Serializable
    data class Asset(
        val url: String,
        val name: String,
        val size: Long,
        val digest: String,
        @SerialName("browser_download_url")
        val browserDownloadUrl: String,
    )

    fun asset(target: SkiaTarget): Asset? {
        return assets.find { it.name == "Skia-${tagName}-${target.os}-${target.buildType}-${target.arch}.zip" }
    }
}

@Serializable
private data class NixVersionsInput(
    val version: String,
    val platforms: Map<NixPlatform, PlatformAsset>,
) {
    @Serializable(with = NixPlatformSerializer::class)
    data class NixPlatform(val arch: NixArch, val os: SkiaOs) {
        constructor(skiaTarget: SkiaTarget) : this(
            skiaTarget.arch.nix ?: error("unsupported arch: ${skiaTarget.arch}"),
            skiaTarget.os,
        )
    }

    object NixPlatformSerializer : KSerializer<NixPlatform> {
        override val descriptor: SerialDescriptor = serialDescriptor<String>()

        override fun deserialize(decoder: Decoder): NixPlatform {
            val value = decoder.decodeString()
            val components = value.split('-')
            if (components.size != 2) {
                throw SerializationException("Invalid input for nix platform: ${value}")
            }
            return NixPlatform(NixArch.valueOf(components[0]), SkiaOs.valueOf(components[1]))
        }

        override fun serialize(encoder: Encoder, value: NixPlatform) {
            encoder.encodeString("${value.arch}-${value.os}")
        }
    }

    @Serializable
    data class PlatformAsset(
        @Serializable(with = DefaultSkiaTargetSerializer::class)
        val target: SkiaTarget,
        val hash: String,
    )
}

abstract class PrepareSkiaVersionsJson : DefaultTask() {
    @get:Classpath
    abstract val skikoConfiguration: ConfigurableFileCollection

    @get:Input
    abstract val skiaTargets: SetProperty<SkiaTarget>

    @get:Input
    @get:Optional
    abstract val githubToken: Property<String>

    @get:OutputFile
    abstract val versionsJson: RegularFileProperty

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    @Suppress("UNCHECKED_CAST")
    fun generate() {
        val classpath = skikoConfiguration.files.map { it.toURI().toURL() }
        val (skikoVersion, skiaVersion) = URLClassLoader
            .newInstance(classpath.toTypedArray(), javaClass.classLoader)
            .use { skikoClassLoader ->
                val versionClass = skikoClassLoader
                    .loadClass("org.jetbrains.skiko.Version")
                    .kotlin
                val versionObj = versionClass.objectInstance as Any

                val skikoVersionProperty =
                    versionClass.memberProperties.first { it.name == "skiko" } as KProperty1<Any, String>
                val skiaVersionProperty =
                    versionClass.memberProperties.first { it.name == "skia" } as KProperty1<Any, String>
                val skikoVersion = skikoVersionProperty.get(versionObj)
                val skiaVersion = skiaVersionProperty.get(versionObj)

                skikoVersion to skiaVersion
            }
        logger.lifecycle("Skiko versions: (skiko=$skikoVersion, skia=$skiaVersion)")

        val skiaRelease = resolveSkiaRelease(skiaVersion)
        val targetArchives = skiaTargets.get().associateWith {
            skiaRelease.asset(it) ?: error("missing release asset for $it")
        }
        val nixAssets = targetArchives
            .mapValues { NixVersionsInput.PlatformAsset(it.key, it.value.digest) }
            .mapKeys { NixVersionsInput.NixPlatform(it.key) }
        val result = NixVersionsInput(skiaVersion, nixAssets)
        versionsJson.get().asFile.outputStream().use {
            json.encodeToStream(result, it)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun resolveSkiaRelease(version: String): GithubRelease {
        val request =
            HttpRequest.newBuilder(URI.create("https://api.github.com/repos/JetBrains/skia/releases/tags/${version}"))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2026-03-10")
        githubToken.orNull?.let {
            request.header("Authorization", "Bearer $it")
        }
        return HttpClient.newHttpClient().use { client ->
            val response = client.send(request.GET().build(), HttpResponse.BodyHandlers.ofInputStream())
            check(response.statusCode() == 200) {
                "Failed to get skia release $version: HTTP ${response.statusCode()}"
            }
            response.body().use(json::decodeFromStream)
        }
    }
}
