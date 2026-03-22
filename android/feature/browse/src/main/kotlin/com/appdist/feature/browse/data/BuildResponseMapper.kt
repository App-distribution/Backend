package com.appdist.feature.browse.data

import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.network.dto.BuildResponse

internal fun BuildResponse.toUi() = BuildUi(
    id = id, projectId = projectId, projectName = "", packageName = "",
    versionName = versionName, versionCode = versionCode,
    channel = channel, environment = environment, buildType = buildType,
    changelog = changelog, fileSize = fileSize, checksumSha256 = checksumSha256,
    status = status, isLatestInChannel = isLatestInChannel,
    uploadDate = uploadDate, uploaderName = uploaderName,
    minSdk = minSdk, targetSdk = targetSdk, certFingerprint = certFingerprint,
    abis = abis ?: emptyList(), expiryDate = expiryDate,
    branch = branch, commitHash = commitHash,
    installStatus = InstallStatus.NotInstalled
)
