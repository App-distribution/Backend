package com.appdist.infrastructure.apk

import net.dongliu.apk.parser.ApkFile
import java.io.File
import java.security.MessageDigest

data class ApkMetadata(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val abis: List<String>,
    val certFingerprint: String?,
    val fileSizeBytes: Long,
    val checksumSha256: String,
)

object ApkMetadataExtractor {
    fun extract(file: File): ApkMetadata {
        val sha256 = file.inputStream().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }

        return ApkFile(file).use { apkFile ->
            val info = apkFile.apkMeta
            val certFingerprint = runCatching {
                apkFile.apkSingers.firstOrNull()
                    ?.certificateMetas?.firstOrNull()
                    ?.certMd5
            }.getOrNull()

            ApkMetadata(
                packageName = info.packageName,
                versionName = info.versionName ?: "unknown",
                versionCode = info.versionCode ?: 0L,
                minSdk = info.minSdkVersion?.toIntOrNull() ?: 1,
                targetSdk = info.targetSdkVersion?.toIntOrNull() ?: 1,
                abis = emptyList(),
                certFingerprint = certFingerprint,
                fileSizeBytes = file.length(),
                checksumSha256 = sha256,
            )
        }
    }
}
