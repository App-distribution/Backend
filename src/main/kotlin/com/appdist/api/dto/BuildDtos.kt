package com.appdist.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class BuildDto(
    val id: String,
    val projectId: String,
    val versionName: String,
    val versionCode: Long,
    val buildNumber: String?,
    val flavor: String?,
    val buildType: String,
    val environment: String,
    val channel: String,
    val branch: String?,
    val commitHash: String?,
    val uploaderName: String,
    val uploadDate: Long,          // epoch millis
    val changelog: String?,
    val fileSize: Long,
    val checksumSha256: String,
    val minSdk: Int,
    val targetSdk: Int,
    val certFingerprint: String?,
    val abis: List<String>,
    val status: String,
    val expiryDate: Long?,         // epoch millis
    val isLatestInChannel: Boolean,
)

@Serializable
data class UpdateBuildRequest(
    val changelog: String? = null,
    val status: String? = null,
)

@Serializable
data class DownloadUrlResponse(val url: String, val expiresAt: Long)

fun com.appdist.domain.model.Build.toDto() = BuildDto(
    id = id.toString(),
    projectId = projectId.toString(),
    versionName = versionName,
    versionCode = versionCode,
    buildNumber = buildNumber,
    flavor = flavor,
    buildType = buildType,
    environment = environment.name,
    channel = channel.name,
    branch = branch,
    commitHash = commitHash,
    uploaderName = "",
    uploadDate = uploadDate.toEpochMilliseconds(),
    changelog = changelog,
    fileSize = fileSizeBytes,
    checksumSha256 = checksumSha256,
    minSdk = minSdk,
    targetSdk = targetSdk,
    certFingerprint = certFingerprint,
    abis = abis,
    status = status.name,
    expiryDate = expiryDate?.toEpochMilliseconds(),
    isLatestInChannel = isLatestInChannel,
)
