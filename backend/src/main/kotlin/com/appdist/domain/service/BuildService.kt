package com.appdist.domain.service

import com.appdist.config.AppConfig
import com.appdist.domain.model.*
import com.appdist.domain.repository.*
import com.appdist.infrastructure.apk.ApkMetadataExtractor
import com.appdist.infrastructure.storage.StorageClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import java.io.File
import java.util.UUID

private val log = KotlinLogging.logger {}

data class UploadRequest(
    val projectId: UUID,
    val uploaderId: UUID,
    val environment: BuildEnvironment,
    val channel: ReleaseChannel,
    val buildType: String,
    val flavor: String?,
    val branch: String?,
    val commitHash: String?,
    val changelog: String?,
    val file: File,
    val originalFileName: String,
)

class BuildService(
    private val buildRepository: BuildRepository,
    private val storageClient: StorageClient,
    private val auditRepository: AuditRepository,
    private val storageConfig: AppConfig.StorageConfig,
    private val projectRepository: ProjectRepository? = null,
    private val notificationService: NotificationService? = null,
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun uploadBuild(request: UploadRequest): Build {
        val metadata = ApkMetadataExtractor.extract(request.file)

        val existing = buildRepository.findByChecksum(metadata.checksumSha256)
        if (existing != null) {
            error("Build with checksum ${metadata.checksumSha256} already exists (id: ${existing.id})")
        }

        val buildId = UUID.randomUUID()
        val storageKey = "${request.projectId}/${buildId}/app.apk"

        storageClient.upload(
            key = storageKey,
            inputStream = request.file.inputStream(),
            size = metadata.fileSizeBytes,
            contentType = "application/vnd.android.package-archive"
        )

        buildRepository.unsetLatestInChannel(request.projectId, request.channel)

        val build = Build(
            id = buildId,
            projectId = request.projectId,
            versionName = metadata.versionName,
            versionCode = metadata.versionCode,
            buildNumber = null,
            flavor = request.flavor,
            buildType = request.buildType,
            environment = request.environment,
            channel = request.channel,
            branch = request.branch,
            commitHash = request.commitHash,
            uploaderId = request.uploaderId,
            uploadDate = Clock.System.now(),
            changelog = request.changelog,
            fileSizeBytes = metadata.fileSizeBytes,
            checksumSha256 = metadata.checksumSha256,
            minSdk = metadata.minSdk,
            targetSdk = metadata.targetSdk,
            certFingerprint = metadata.certFingerprint,
            abis = metadata.abis,
            storageKey = storageKey,
            status = BuildStatus.ACTIVE,
            expiryDate = null,
            isLatestInChannel = true,
        )

        val saved = buildRepository.create(build)

        // Fire-and-forget FCM notification
        if (notificationService != null && projectRepository != null) {
            serviceScope.launch {
                runCatching {
                    val project = projectRepository.findById(request.projectId)
                    if (project != null) {
                        notificationService.notifyNewBuild(saved, project.name, project.workspaceId)
                    }
                }.onFailure { log.warn(it) { "FCM notification failed for build ${saved.id}" } }
            }
        }

        // Audit log — fire-and-forget
        serviceScope.launch {
            runCatching {
                auditRepository.log(
                    userId = request.uploaderId,
                    action = "build.upload",
                    resourceType = "build",
                    resourceId = buildId,
                    metadata = mapOf("version" to metadata.versionName, "channel" to request.channel.name)
                )
            }.onFailure { log.warn(it) { "Audit log failed for build.upload" } }
        }

        return saved
    }

    suspend fun getDownloadUrl(buildId: UUID, requesterId: UUID): String {
        val build = buildRepository.findById(buildId) ?: error("Build not found")
        val url = storageClient.generateDownloadUrl(build.storageKey, ttlMinutes = 15L)
        serviceScope.launch {
            runCatching { auditRepository.log(requesterId, "build.download_url", "build", buildId) }
        }
        return url
    }

    suspend fun listBuilds(
        projectId: UUID, channel: ReleaseChannel?, search: String?, page: Int, limit: Int
    ): List<Build> = buildRepository.listByProject(projectId, channel, search, page, limit)

    suspend fun getBuild(buildId: UUID): Build =
        buildRepository.findById(buildId) ?: error("Build not found")

    suspend fun updateBuild(buildId: UUID, changelog: String?, status: BuildStatus, requesterId: UUID): Build {
        val build = buildRepository.update(buildId, changelog, status)
        serviceScope.launch {
            runCatching {
                auditRepository.log(requesterId, "build.update", "build", buildId, mapOf("status" to status.name))
            }
        }
        return build
    }

    suspend fun deleteBuild(buildId: UUID, requesterId: UUID) {
        val build = buildRepository.findById(buildId) ?: error("Build not found")
        storageClient.delete(build.storageKey)
        buildRepository.delete(buildId)
        serviceScope.launch {
            runCatching { auditRepository.log(requesterId, "build.delete", "build", buildId) }
        }
    }
}
