package com.appdist

import com.appdist.api.routes.*
import com.appdist.config.AppConfig
import com.appdist.domain.service.ApiKeyService
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.NotificationService
import com.appdist.infrastructure.database.repository.*
import com.appdist.infrastructure.storage.StorageClient
import com.appdist.plugins.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.mockk.mockk

fun Application.testModule() {
    TestDatabase.init()
    configureSerialization()
    configureStatusPages()

    val jwtConfig = AppConfig.JwtConfig(
        "test-secret-key-for-testing-only-32chars-min",
        "appdist", "appdist-client", 60L, 30L
    )
    val otpConfig = AppConfig.OtpConfig(5L, 6)

    val userRepo = UserRepositoryImpl()
    val workspaceRepo = WorkspaceRepositoryImpl()
    val auditRepo = AuditRepositoryImpl()
    val projectRepo = ProjectRepositoryImpl()
    val buildRepo = BuildRepositoryImpl()

    val authService = AuthService(
        userRepo, workspaceRepo,
        OtpRepositoryImpl(), RefreshTokenRepositoryImpl(),
        jwtConfig, otpConfig,
        auditRepository = auditRepo
    )
    val apiKeyService = ApiKeyService(ApiKeyRepositoryImpl(), userRepo, auditRepo)
    configureAuth(jwtConfig, apiKeyService)

    // MinioStorageClient проверяет бакет в init{} и требует живого Minio —
    // в юнит-тестах (testModule используют 6 файлов, не только этот) его нет.
    // Загрузка по ключу здесь всё равно не доходит до storageClient без
    // фикстуры test.apk, поэтому relaxed-мок достаточно, чтобы роут
    // существовал и не падал на старте приложения.
    val storageConfig = AppConfig.StorageConfig(
        endpoint = "http://localhost:9000",
        publicEndpoint = "http://localhost:9000",
        accessKey = "minioadmin",
        secretKey = "minioadmin",
        bucket = "test-bucket",
    )
    val buildService = BuildService(
        buildRepository = buildRepo,
        storageClient = mockk<StorageClient>(relaxed = true),
        auditRepository = auditRepo,
        storageConfig = storageConfig,
        projectRepository = projectRepo,
        notificationService = NotificationService(userRepo),
    )

    routing {
        route("/api/v1") {
            authRoutes(authService)
            uploadRoutes(buildService)
            projectRoutes(projectRepo, auditRepo, workspaceRepo)
            workspaceRoutes(workspaceRepo)
            userRoutes(userRepo)
            apiKeyRoutes(apiKeyService)
        }
    }
}
