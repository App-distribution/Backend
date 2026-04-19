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
import kotlinx.coroutines.runBlocking

abstract class IntegrationTestBase {
    companion object {
        val testJwtConfig = AppConfig.JwtConfig(
            secret = "integration-test-secret-key-32-chars-min",
            issuer = "appdist",
            audience = "appdist-client",
            accessTokenTtlMinutes = 60L,
            refreshTokenTtlDays = 30L,
        )

        const val TEST_ADMIN_EMAIL = "admin@integration-test.com"
        const val TEST_ADMIN_PASSWORD = "integrationtestpassword"

        init {
            IntegrationContainers.initDatabase()
        }
    }

    fun Application.integrationTestModule() {
        // DatabaseFactory.init() also calls createTables() internally
        DatabaseFactory.init(IntegrationContainers.dbConfig)

        val userRepo = UserRepositoryImpl()
        val workspaceRepo = WorkspaceRepositoryImpl()
        val auditRepo = AuditRepositoryImpl()
        val projectRepo = ProjectRepositoryImpl()
        val refreshTokenRepo = RefreshTokenRepositoryImpl()

        val authService = AuthService(
            userRepo, workspaceRepo, refreshTokenRepo,
            testJwtConfig,
            auditRepository = auditRepo,
        )

        // Seed admin user for integration tests (idempotent)
        runBlocking { authService.bootstrapAdmin(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD) }

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
                userManagementRoutes(authService, userRepo)
            }
        }
    }
}
