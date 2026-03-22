package com.appdist.core.common.model

data class BuildUi(
    val id: String,
    val projectId: String,
    val projectName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val channel: String,
    val environment: String,
    val buildType: String,
    val changelog: String?,
    val fileSize: Long,
    val checksumSha256: String,
    val status: String,
    val isLatestInChannel: Boolean,
    val uploadDate: Long,
    val uploaderName: String,
    val minSdk: Int,
    val targetSdk: Int,
    val certFingerprint: String?,
    val abis: List<String>,
    val expiryDate: Long?,
    val branch: String?,
    val commitHash: String?,
    val installStatus: InstallStatus = InstallStatus.NotInstalled
)

sealed interface InstallStatus {
    data object NotInstalled : InstallStatus
    data class Installed(val versionCode: Long) : InstallStatus
    data class UpdateAvailable(val installed: Long, val available: Long) : InstallStatus
    data class InstalledNewer(val installed: Long, val available: Long) : InstallStatus
    data class SignatureMismatch(val installedFingerprint: String) : InstallStatus
    data object Incompatible : InstallStatus
}
