package com.appdist.plugins

import com.appdist.api.routes.*
import com.appdist.config.AppConfig
import com.appdist.domain.service.ApiKeyService
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.NotificationService
import com.appdist.infrastructure.database.repository.*
import com.appdist.infrastructure.email.LogEmailSender
import com.appdist.infrastructure.email.SmtpEmailSender
import com.appdist.infrastructure.storage.MinioStorageClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.*
import io.ktor.server.routing.*

private val logger = KotlinLogging.logger {}

fun Application.configureRouting(config: AppConfig, apiKeyService: ApiKeyService) {
    val userRepo = UserRepositoryImpl()
    val workspaceRepo = WorkspaceRepositoryImpl()
    val otpRepo = OtpRepositoryImpl()
    val refreshTokenRepo = RefreshTokenRepositoryImpl()
    val auditRepo = AuditRepositoryImpl()
    val buildRepo = BuildRepositoryImpl()
    val projectRepo = ProjectRepositoryImpl()

    val emailSender = config.smtp
        ?.let { logger.info { "OTP emails via SMTP ${it.host}:${it.port} as ${it.from}" }; SmtpEmailSender(it) }
        ?: LogEmailSender().also { logger.warn { "SMTP_HOST not set — OTP codes go to the log, do not use in production" } }

    val authService = AuthService(
        userRepository = userRepo,
        workspaceRepository = workspaceRepo,
        otpRepository = otpRepo,
        refreshTokenRepository = refreshTokenRepo,
        jwtConfig = config.jwt,
        otpConfig = config.otp,
        auditRepository = auditRepo,
        emailSender = emailSender,
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
            publicRoutes(projectRepo, buildService)
            authRoutes(authService)
            buildRoutes(buildService)
            uploadRoutes(buildService)
            projectRoutes(projectRepo, auditRepo, workspaceRepo)
            workspaceRoutes(workspaceRepo)
            userRoutes(userRepo)
            apiKeyRoutes(apiKeyService)
        }
    }
}
