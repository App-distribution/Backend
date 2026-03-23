package com.appdist.integration

import com.appdist.config.AppConfig
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.NotificationService
import com.appdist.infrastructure.database.DatabaseFactory
import com.appdist.infrastructure.database.repository.*
import com.appdist.infrastructure.storage.MinioStorageClient
import com.appdist.plugins.*
import com.appdist.api.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import java.util.concurrent.ConcurrentHashMap

abstract class IntegrationTestBase {
    companion object {
        // Capture OTPs for test use
        private val capturedOtps = ConcurrentHashMap<String, String>()
        fun recordOtp(email: String, otp: String) { capturedOtps[email] = otp }
        fun captureLastOtp(email: String): String = capturedOtps[email] ?: error("No OTP captured for $email")

        val testJwtConfig = AppConfig.JwtConfig(
            secret = "integration-test-secret-key-32-chars-min",
            issuer = "appdist",
            audience = "appdist-client",
            accessTokenTtlMinutes = 60L,
            refreshTokenTtlDays = 30L,
        )
        val testOtpConfig = AppConfig.OtpConfig(ttlMinutes = 5L, length = 6)
    }

    fun Application.integrationTestModule() {
        // DatabaseFactory.init() also calls createTables() internally
        DatabaseFactory.init(IntegrationContainers.dbConfig)

        val userRepo = UserRepositoryImpl()
        val workspaceRepo = WorkspaceRepositoryImpl()
        val auditRepo = AuditRepositoryImpl()
        val projectRepo = ProjectRepositoryImpl()

        // AuthService with OTP capture hook for tests
        val authService = object : AuthService(
            userRepo, workspaceRepo,
            OtpRepositoryImpl(), RefreshTokenRepositoryImpl(),
            testJwtConfig, testOtpConfig,
            auditRepository = auditRepo,
        ) {
            override suspend fun requestOtp(email: String): String {
                val otp = super.requestOtp(email)
                recordOtp(email, otp)
                return otp
            }
        }

        configureSerialization()
        configureStatusPages()
        configureAuth(testJwtConfig)

        val storageClient = MinioStorageClient(IntegrationContainers.storageConfig)
        val buildRepo = BuildRepositoryImpl()
        val buildService = BuildService(
            buildRepository = buildRepo,
            storageClient = storageClient,
            auditRepository = auditRepo,
            storageConfig = IntegrationContainers.storageConfig,
            projectRepository = projectRepo,
            notificationService = NotificationService(userRepo),
        )

        routing {
            route("/api/v1") {
                authRoutes(authService)
                workspaceRoutes(workspaceRepo)
                buildRoutes(buildService)
                uploadRoutes(buildService)
                projectRoutes(projectRepo, auditRepo, workspaceRepo)
                userRoutes(userRepo)
            }
        }
    }
}
