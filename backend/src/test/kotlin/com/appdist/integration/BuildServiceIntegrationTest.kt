package com.appdist.integration

import com.appdist.domain.model.BuildEnvironment
import com.appdist.domain.model.ReleaseChannel
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.UploadRequest
import com.appdist.infrastructure.apk.ApkMetadata
import com.appdist.infrastructure.database.repository.*
import com.appdist.infrastructure.storage.MinioStorageClient
import kotlinx.coroutines.test.runTest
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.test.*

/**
 * Service-level integration tests for BuildService using real PostgreSQL and MinIO containers.
 * Containers are shared via [IntegrationContainers].
 */
class BuildServiceIntegrationTest {

    companion object {
        init {
            // Ensure containers are started and DB is initialized before any tests run
            IntegrationContainers.initDatabase()
        }

        /**
         * Creates a temporary file with the given content and computes its SHA-256 checksum.
         * Returns Pair(file, checksum).
         */
        fun createFakeApkFile(content: String): Pair<File, String> {
            val file = File.createTempFile("test", ".apk")
            file.deleteOnExit()
            file.writeText(content)
            val bytes = file.readBytes()
            val digest = MessageDigest.getInstance("SHA-256")
            val checksum = digest.digest(bytes).joinToString("") { "%02x".format(it) }
            return file to checksum
        }
    }

    private lateinit var workspaceId: UUID
    private lateinit var projectId: UUID
    private lateinit var uploaderId: UUID

    @BeforeTest
    fun setup() = runTest {
        val workspaceRepo = WorkspaceRepositoryImpl()
        val userRepo = UserRepositoryImpl()
        val projectRepo = ProjectRepositoryImpl()

        val uniqueSuffix = System.currentTimeMillis()
        val ws = workspaceRepo.create(
            name = "bs-test-workspace-$uniqueSuffix",
            slug = "bs-test-ws-$uniqueSuffix",
        )
        workspaceId = ws.id

        val user = userRepo.create(
            workspaceId = ws.id,
            email = "uploader-$uniqueSuffix@bs-test.com",
            name = "Test Uploader",
            role = UserRole.ADMIN,
        )
        uploaderId = user.id

        val project = projectRepo.create(
            workspaceId = ws.id,
            name = "BSTestApp-$uniqueSuffix",
            packageName = "com.bstest.app",
        )
        projectId = project.id
    }

    private fun buildServiceWithFakeExtractor(fakeMetadata: ApkMetadata): BuildService {
        val storageConfig = IntegrationContainers.storageConfig
        return BuildService(
            buildRepository = BuildRepositoryImpl(),
            storageClient = MinioStorageClient(storageConfig),
            auditRepository = AuditRepositoryImpl(),
            storageConfig = storageConfig,
            projectRepository = ProjectRepositoryImpl(),
            apkExtractor = { fakeMetadata },
        )
    }

    @Test
    fun `uploadBuild stores build metadata in DB and returns build`() = runTest {
        val (apkFile, checksum) = createFakeApkFile("fake-apk-upload-1-${System.currentTimeMillis()}")
        val fakeMetadata = ApkMetadata(
            packageName = "com.bstest.app",
            versionName = "1.0.0",
            versionCode = 1L,
            minSdk = 24,
            targetSdk = 34,
            abis = listOf("arm64-v8a"),
            certFingerprint = null,
            fileSizeBytes = apkFile.length(),
            checksumSha256 = checksum,
        )

        val service = buildServiceWithFakeExtractor(fakeMetadata)

        val build = service.uploadBuild(
            UploadRequest(
                projectId = projectId,
                uploaderId = uploaderId,
                environment = BuildEnvironment.QA,
                channel = ReleaseChannel.INTERNAL,
                buildType = "debug",
                flavor = null,
                branch = "main",
                commitHash = null,
                changelog = "Initial build",
                file = apkFile,
                originalFileName = "test.apk",
            )
        )

        assertNotNull(build.id)
        assertEquals("1.0.0", build.versionName)
        assertEquals(1L, build.versionCode)
        assertEquals(projectId, build.projectId)
        assertEquals(checksum, build.checksumSha256)
        assertEquals(ReleaseChannel.INTERNAL, build.channel)
        assertEquals(BuildEnvironment.QA, build.environment)
        assertTrue(build.isLatestInChannel)
    }

    @Test
    fun `listBuilds returns uploaded build`() = runTest {
        val (apkFile, checksum) = createFakeApkFile("fake-apk-list-${System.currentTimeMillis()}")
        val fakeMetadata = ApkMetadata(
            packageName = "com.bstest.app",
            versionName = "2.0.0",
            versionCode = 2L,
            minSdk = 24,
            targetSdk = 34,
            abis = emptyList(),
            certFingerprint = null,
            fileSizeBytes = apkFile.length(),
            checksumSha256 = checksum,
        )

        val service = buildServiceWithFakeExtractor(fakeMetadata)

        service.uploadBuild(
            UploadRequest(
                projectId = projectId,
                uploaderId = uploaderId,
                environment = BuildEnvironment.DEV,
                channel = ReleaseChannel.ALPHA,
                buildType = "debug",
                flavor = null,
                branch = null,
                commitHash = null,
                changelog = null,
                file = apkFile,
                originalFileName = "test.apk",
            )
        )

        val builds = service.listBuilds(projectId, ReleaseChannel.ALPHA, null, 0, 10)
        assertTrue(builds.isNotEmpty())
        assertTrue(builds.any { it.checksumSha256 == checksum })
    }

    @Test
    fun `getDownloadUrl succeeds for uploaded build`() = runTest {
        val (apkFile, checksum) = createFakeApkFile("fake-apk-download-${System.currentTimeMillis()}")
        val fakeMetadata = ApkMetadata(
            packageName = "com.bstest.app",
            versionName = "3.0.0",
            versionCode = 3L,
            minSdk = 21,
            targetSdk = 33,
            abis = emptyList(),
            certFingerprint = null,
            fileSizeBytes = apkFile.length(),
            checksumSha256 = checksum,
        )

        val service = buildServiceWithFakeExtractor(fakeMetadata)

        val build = service.uploadBuild(
            UploadRequest(
                projectId = projectId,
                uploaderId = uploaderId,
                environment = BuildEnvironment.STAGING,
                channel = ReleaseChannel.BETA,
                buildType = "release",
                flavor = null,
                branch = null,
                commitHash = null,
                changelog = null,
                file = apkFile,
                originalFileName = "test.apk",
            )
        )

        val url = service.getDownloadUrl(build.id, uploaderId)
        assertNotNull(url)
        assertTrue(url.startsWith("http"), "Expected a presigned HTTP URL but got: $url")
    }

    @Test
    fun `uploadBuild with duplicate checksum throws error`() = runTest {
        val (apkFile, checksum) = createFakeApkFile("fake-apk-duplicate-${System.currentTimeMillis()}")
        val fakeMetadata = ApkMetadata(
            packageName = "com.bstest.app",
            versionName = "4.0.0",
            versionCode = 4L,
            minSdk = 24,
            targetSdk = 34,
            abis = emptyList(),
            certFingerprint = null,
            fileSizeBytes = apkFile.length(),
            checksumSha256 = checksum,
        )

        val service = buildServiceWithFakeExtractor(fakeMetadata)

        val request = UploadRequest(
            projectId = projectId,
            uploaderId = uploaderId,
            environment = BuildEnvironment.QA,
            channel = ReleaseChannel.NIGHTLY,
            buildType = "debug",
            flavor = null,
            branch = null,
            commitHash = null,
            changelog = null,
            file = apkFile,
            originalFileName = "test.apk",
        )

        service.uploadBuild(request)

        val exception = assertFailsWith<IllegalStateException> {
            service.uploadBuild(request)
        }
        assertTrue(
            exception.message?.contains(checksum) == true || exception.message?.contains("already exists") == true,
            "Expected duplicate checksum error, got: ${exception.message}"
        )
    }
}
