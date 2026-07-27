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
        if (allowMissingMapping.get()) {
            logger.warn("[dutype] Release mapping validation skipped by com.dutype.allowMissingReleaseMapping=true")
            return
        }

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

abstract class VerifyNative16KbPageSizeTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val aabFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val aab = aabFile.asFile.get()
        if (!aab.exists()) {
            throw GradleException("Release AAB not found at ${aab.path}. Run :app:bundleRelease first.")
        }

        ZipFile(aab).use { zipFile ->
            val nativeEntries = zipFile.entries().asSequence()
                .filter { it.name.startsWith("base/lib/") && it.name.endsWith(".so") }
                .toList()

            if (nativeEntries.isEmpty()) {
                logger.lifecycle("[dutype] 16 KB native library check: no native libraries in release AAB")
                return
            }

            val failures = mutableListOf<String>()
            nativeEntries.forEach { entry ->
                val loadAlignments = zipFile.getInputStream(entry).use { input ->
                    readElfLoadAlignments(input.readBytes())
                }
                val undersized = loadAlignments.filter { it < 16_384L }
                if (undersized.isNotEmpty()) {
                    failures += "${entry.name} has LOAD alignment(s) ${undersized.toHexList()}; expected at least 0x4000"
                }
            }

            if (failures.isNotEmpty()) {
                throw GradleException(
                    "Release native library 16 KB page-size check failed:\n" +
                        failures.joinToString(separator = "\n") { "- $it" } +
                        "\nUpdate or rebuild the offending native dependency before uploading to Play."
                )
            }

            logger.lifecycle(
                "[dutype] 16 KB native library check OK: " +
                    nativeEntries.joinToString { it.name.removePrefix("base/lib/") }
            )
        }
    }
}

private fun Long.toMiBString(): String = String.format(Locale.US, "%.2f MB", this / (1024.0 * 1024.0))

private fun List<Long>.toHexList(): String = joinToString { "0x${it.toString(16)}" }

private fun readElfLoadAlignments(bytes: ByteArray): List<Long> {
    if (
        bytes.size < 64 ||
        bytes[0] != 0x7f.toByte() ||
        bytes[1] != 'E'.code.toByte() ||
        bytes[2] != 'L'.code.toByte() ||
        bytes[3] != 'F'.code.toByte()
    ) {
        throw GradleException("Native library is not a valid ELF file")
    }

    val elfClass = bytes[4].toInt()
    val isLittleEndian = bytes[5].toInt() == 1
    fun u16(offset: Int): Int {
        val b0 = bytes[offset].toInt() and 0xff
        val b1 = bytes[offset + 1].toInt() and 0xff
        return if (isLittleEndian) b0 or (b1 shl 8) else (b0 shl 8) or b1
    }
    fun u32(offset: Int): Long {
        val values = (0 until 4).map { bytes[offset + it].toLong() and 0xffL }
        return if (isLittleEndian) {
            values[0] or (values[1] shl 8) or (values[2] shl 16) or (values[3] shl 24)
        } else {
            (values[0] shl 24) or (values[1] shl 16) or (values[2] shl 8) or values[3]
        }
    }
    fun u64(offset: Int): Long {
        val values = (0 until 8).map { bytes[offset + it].toLong() and 0xffL }
        return if (isLittleEndian) {
            values.foldIndexed(0L) { index, acc, value -> acc or (value shl (8 * index)) }
        } else {
            values.fold(0L) { acc, value -> (acc shl 8) or value }
        }
    }

    return when (elfClass) {
        1 -> {
            val programHeaderOffset = u32(28).toInt()
            val programHeaderSize = u16(42)
            val programHeaderCount = u16(44)
            (0 until programHeaderCount).mapNotNull { index ->
                val offset = programHeaderOffset + index * programHeaderSize
                if (u32(offset) == 1L) u32(offset + 28) else null
            }
        }
        2 -> {
            val programHeaderOffset = u64(32).toInt()
            val programHeaderSize = u16(54)
            val programHeaderCount = u16(56)
            (0 until programHeaderCount).mapNotNull { index ->
                val offset = programHeaderOffset + index * programHeaderSize
                if (u32(offset) == 1L) u64(offset + 48) else null
            }
        }
        else -> throw GradleException("Unsupported ELF class: $elfClass")
    }
}

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
