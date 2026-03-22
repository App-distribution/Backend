package com.appdist.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuildResponse(
    @SerialName("id") val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("version_name") val versionName: String,
    @SerialName("version_code") val versionCode: Long,
    @SerialName("build_number") val buildNumber: String? = null,
    @SerialName("flavor") val flavor: String? = null,
    @SerialName("build_type") val buildType: String,
    @SerialName("environment") val environment: String,
    @SerialName("channel") val channel: String,
    @SerialName("branch") val branch: String? = null,
    @SerialName("commit_hash") val commitHash: String? = null,
    @SerialName("uploader_name") val uploaderName: String,
    @SerialName("upload_date") val uploadDate: Long,
    @SerialName("changelog") val changelog: String? = null,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("checksum_sha256") val checksumSha256: String,
    @SerialName("min_sdk") val minSdk: Int,
    @SerialName("target_sdk") val targetSdk: Int,
    @SerialName("cert_fingerprint") val certFingerprint: String? = null,
    @SerialName("abis") val abis: List<String>? = null,
    @SerialName("status") val status: String,
    @SerialName("expiry_date") val expiryDate: Long? = null,
    @SerialName("is_latest_in_channel") val isLatestInChannel: Boolean
)

@Serializable
data class DownloadUrlResponse(
    @SerialName("url") val url: String,
    @SerialName("expires_at") val expiresAt: Long
)

@Serializable
data class InstallEventRequest(
    @SerialName("device_model") val deviceModel: String,
    @SerialName("android_version") val androidVersion: String,
    @SerialName("sdk_version") val sdkVersion: Int
)
