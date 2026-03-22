package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class DeviceInfo(
    val model: String?,
    val androidVersion: String?,
    val manufacturer: String?,
)

data class InstallEvent(
    val id: UUID,
    val userId: UUID,
    val buildId: UUID,
    val installedAt: Instant,
    val deviceInfo: DeviceInfo,
)
