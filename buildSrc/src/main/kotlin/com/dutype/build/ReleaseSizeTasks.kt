package com.dutype.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipFile

abstract class ValidateReleaseMappingBaselineTask : DefaultTask() {
    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mappingFile: RegularFileProperty

    @get:Input
    abstract val allowMissingMapping: Property<Boolean>

    @get:Input
    abstract val expectedSha256: Property<String>

    @get:Internal
    abstract val repoRoot: DirectoryProperty

    @TaskAction
    fun validate() {
        val mapping = mappingFile.asFile.get()

        if (!mapping.exists()) {
            if (allowMissingMapping.get()) {
                logger.warn("[dutype] Missing release mapping allowed by -Pcom.dutype.allowMissingReleaseMapping=true")
                return
            }
            throw GradleException(
                "app/mapping/release-mapping.txt is missing. Do not upload a Play release without the " +
                    "currently live production R8 mapping, or tiny changes can become multi-MB updates."
            )
        }

        if (mapping.length() < 1_000_000L) {
            throw GradleException(
                "app/mapping/release-mapping.txt is only ${mapping.length()} bytes. It looks like a Git LFS " +
                    "pointer or corrupt mapping, not the real production mapping. Run `git lfs pull` before building."
            )
        }

        val expectedHash = expectedSha256.get().trim().uppercase(Locale.US)
        if (expectedHash.isNotBlank()) {
            val actualHash = mapping.sha256().uppercase(Locale.US)
            if (actualHash != expectedHash) {
                throw GradleException(
                    "app/mapping/release-mapping.txt does not match the expected production baseline hash. " +
                        "Do not upload this AAB for a tiny Play hotfix; it may patch dex in MBs instead of KBs.\n" +
                        "Expected: $expectedHash\nActual:   $actualHash\n" +
                        "If Play has accepted a newer release, update com.dutype.releaseMappingSha256 in gradle.properties " +
                        "only after archiving that accepted release mapping."
                )
            }
        }

        val gitStatusProcess = ProcessBuilder(
            "git",
            "status",
            "--porcelain",
            "--",
            "app/mapping/release-mapping.txt"
        )
            .directory(repoRoot.asFile.get())
            .redirectErrorStream(true)
            .start()
        val gitStatus = gitStatusProcess.inputStream.bufferedReader().readText().trim()
        val gitExitCode = gitStatusProcess.waitFor()

        if (gitExitCode != 0) {
            throw GradleException("Could not verify release mapping git status before Play upload:\n$gitStatus")
        }

        if (gitStatus.isNotBlank()) {
            throw GradleException(
                "app/mapping/release-mapping.txt has local changes. The Play upload build must consume the " +
                    "currently live production mapping. Commit mapping changes only after Play accepts the matching AAB.\n" +
                    gitStatus
            )
        }

        logger.lifecycle("[dutype] Release mapping baseline OK: ${mapping.length().toMiBString()}")
    }
}

abstract class CheckReleaseSizeBudgetTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val aabFile: RegularFileProperty

    @get:Input
    abstract val maxAabBytes: Property<Long>

    @get:Input
    abstract val maxDexFiles: Property<Int>

    @get:Input
    abstract val maxDexRawBytes: Property<Long>

    @get:Input
    abstract val maxDexCompressedBytes: Property<Long>

    @get:Input
    abstract val maxResourcesBytes: Property<Long>

    @TaskAction
    fun check() {
        val aab = aabFile.asFile.get()
        if (!aab.exists()) {
            throw GradleException("Release AAB not found at ${aab.path}. Run :app:bundleRelease first.")
        }

        ZipFile(aab).use { zipFile ->
            val entries = zipFile.entries().asSequence().toList()
            val dexEntries = entries.filter { it.name.endsWith(".dex") }
            val resourcesEntry = entries.firstOrNull { it.name == "base/resources.pb" }
            val dexRawBytes = dexEntries.sumOf { it.size }
            val dexCompressedBytes = dexEntries.sumOf { it.compressedSize }
            val resourcesBytes = resourcesEntry?.size ?: 0L

            logger.lifecycle(
                "[dutype] Release size budget: AAB=${aab.length().toMiBString()}, " +
                    "dexRaw=${dexRawBytes.toMiBString()}, dexCompressed=${dexCompressedBytes.toMiBString()}, " +
                    "dexFiles=${dexEntries.size}, resources.pb=${resourcesBytes.toMiBString()}"
            )

            val failures = mutableListOf<String>()
            if (aab.length() > maxAabBytes.get()) {
                failures += "AAB ${aab.length().toMiBString()} > budget ${maxAabBytes.get().toMiBString()}"
            }
            if (dexEntries.size > maxDexFiles.get()) {
                failures += "dex file count ${dexEntries.size} > budget ${maxDexFiles.get()}"
            }
            if (dexRawBytes > maxDexRawBytes.get()) {
                failures += "raw dex ${dexRawBytes.toMiBString()} > budget ${maxDexRawBytes.get().toMiBString()}"
            }
            if (dexCompressedBytes > maxDexCompressedBytes.get()) {
                failures += "compressed dex ${dexCompressedBytes.toMiBString()} > budget ${maxDexCompressedBytes.get().toMiBString()}"
            }
            if (resourcesBytes > maxResourcesBytes.get()) {
                failures += "resources.pb ${resourcesBytes.toMiBString()} > budget ${maxResourcesBytes.get().toMiBString()}"
            }

            if (failures.isNotEmpty()) {
                throw GradleException(
                    "Release size budget failed. Do not upload this AAB to Play for a tiny hotfix:\n" +
                        failures.joinToString(separator = "\n") { "- $it" }
                )
            }
        }
    }
}

private fun Long.toMiBString(): String = String.format(Locale.US, "%.2f MB", this / (1024.0 * 1024.0))

private fun java.io.File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
}