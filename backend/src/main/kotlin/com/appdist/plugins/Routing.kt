package com.appdist.plugins

import com.appdist.api.routes.*
import com.appdist.config.AppConfig
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.NotificationService
import com.appdist.infrastructure.database.repository.*
import com.appdist.infrastructure.storage.MinioStorageClient
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(config: AppConfig) {
    val userRepo = UserRepositoryImpl()
    val workspaceRepo = WorkspaceRepositoryImpl()
    val otpRepo = OtpRepositoryImpl()
    val refreshTokenRepo = RefreshTokenRepositoryImpl()
    val auditRepo = AuditRepositoryImpl()
    val buildRepo = BuildRepositoryImpl()
    val projectRepo = ProjectRepositoryImpl()

    val authService = AuthService(
        userRepository = userRepo,
        workspaceRepository = workspaceRepo,
        otpRepository = otpRepo,
        refreshTokenRepository = refreshTokenRepo,
        jwtConfig = config.jwt,
        otpConfig = config.otp,
        auditRepository = auditRepo,
    )

    val storageClient = MinioStorageClient(config.storage)
    val notificationService = NotificationService(userRepo)

    val buildService = BuildService(
        buildRepository = buildRepo,
        storageClient = storageClient,
        auditRepository = auditRepo,
        storageConfig = config.storage,
        projectRepository = projectRepo,
        notificationService = notificationService,
    )

    routing {
        route("/api/v1") {
            authRoutes(authService)
            // build and project routes added in Tasks 10, 11
        }
    }
}
