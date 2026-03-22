package com.appdist.plugins

import com.appdist.api.routes.*
import com.appdist.config.AppConfig
import com.appdist.domain.service.AuthService
import com.appdist.infrastructure.database.repository.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(config: AppConfig) {
    val userRepo = UserRepositoryImpl()
    val workspaceRepo = WorkspaceRepositoryImpl()
    val otpRepo = OtpRepositoryImpl()
    val refreshTokenRepo = RefreshTokenRepositoryImpl()
    val auditRepo = AuditRepositoryImpl()

    val authService = AuthService(
        userRepository = userRepo,
        workspaceRepository = workspaceRepo,
        otpRepository = otpRepo,
        refreshTokenRepository = refreshTokenRepo,
        jwtConfig = config.jwt,
        otpConfig = config.otp,
        auditRepository = auditRepo,
    )

    routing {
        route("/api/v1") {
            authRoutes(authService)
            // more routes added in subsequent tasks
        }
    }
}
