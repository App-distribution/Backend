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
    val uploaderEmail: String?,
    val uploadDate: String,
    val changelog: String?,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val minSdk: Int,
    val targetSdk: Int,
    val certFingerprint: String?,
    val abis: List<String>,
    val status: String,
    val expiryDate: String?,
    val isLatestInChannel: Boolean,
)

@Serializable
data class BuildListResponse(
    val builds: List<BuildDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
)

@Serializable
data class UpdateBuildRequest(
    val changelog: String? = null,
    val status: String? = null,
)

@Serializable
data class DownloadUrlResponse(val url: String, val expiresInMinutes: Int = 15)

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
    uploaderEmail = null, // TODO: populate from uploader join
    uploadDate = uploadDate.toString(),
    changelog = changelog,
    fileSizeBytes = fileSizeBytes,
    checksumSha256 = checksumSha256,
    minSdk = minSdk,
    targetSdk = targetSdk,
    certFingerprint = certFingerprint,
    abis = abis,
    status = status.name,
    expiryDate = expiryDate?.toString(),
    isLatestInChannel = isLatestInChannel,
)
