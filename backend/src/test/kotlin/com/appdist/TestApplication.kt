package com.appdist

import com.appdist.api.routes.*
import com.appdist.config.AppConfig
import com.appdist.domain.service.AuthService
import com.appdist.infrastructure.database.repository.*
import com.appdist.plugins.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

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

    val authService = AuthService(
        userRepo, workspaceRepo,
        OtpRepositoryImpl(), RefreshTokenRepositoryImpl(),
        jwtConfig, otpConfig,
        auditRepository = auditRepo
    )
    configureAuth(jwtConfig)

    routing {
        route("/api/v1") {
            authRoutes(authService)
            projectRoutes(projectRepo, auditRepo, workspaceRepo)
            userRoutes(userRepo)
        }
    }
}
