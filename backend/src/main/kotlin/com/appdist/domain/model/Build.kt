package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

enum class BuildEnvironment { DEV, QA, STAGING, PROD_LIKE }
enum class ReleaseChannel { NIGHTLY, ALPHA, BETA, RC, INTERNAL, CUSTOM }
enum class BuildStatus { ACTIVE, DEPRECATED, ARCHIVED, MANDATORY }

data class Build(
    val id: UUID,
    val projectId: UUID,
    val versionName: String,
    val versionCode: Long,
    val buildNumber: String?,
    val flavor: String?,
    val buildType: String,
    val environment: BuildEnvironment,
    val channel: ReleaseChannel,
    val branch: String?,
    val commitHash: String?,
    val uploaderId: UUID,
    val uploadDate: Instant,
    val changelog: String?,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val minSdk: Int,
    val targetSdk: Int,
    val certFingerprint: String?,
    val abis: List<String>,
    val storageKey: String,
    val status: BuildStatus,
    val expiryDate: Instant?,
    val isLatestInChannel: Boolean,
)
