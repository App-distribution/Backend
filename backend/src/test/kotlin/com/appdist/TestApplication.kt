package com.appdist

import com.appdist.api.routes.*
import com.appdist.config.AppConfig
import com.appdist.domain.service.AuthService
import com.appdist.infrastructure.database.repository.*
import com.appdist.plugins.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

const val TEST_ADMIN_EMAIL = "admin@test.com"
const val TEST_ADMIN_PASSWORD = "testpassword"

fun Application.testModule() {
    TestDatabase.init()    // ensure DB is connected and tables exist before reset
    TestDatabase.reset()   // drop and recreate tables to avoid UNIQUE constraint errors on repeated calls
    configureSerialization()
    configureStatusPages()

    val jwtConfig = AppConfig.JwtConfig(
        "test-secret-key-for-testing-only-32chars-min",
        "appdist", "appdist-client", 60L, 30L
    )

    val userRepo = UserRepositoryImpl()
    val workspaceRepo = WorkspaceRepositoryImpl()
    val auditRepo = AuditRepositoryImpl()
    val projectRepo = ProjectRepositoryImpl()
    val refreshTokenRepo = RefreshTokenRepositoryImpl()

    val authService = AuthService(
        userRepo, workspaceRepo, refreshTokenRepo, jwtConfig,
        auditRepository = auditRepo
    )

    // Seed admin user for tests (bootstrapAdmin is idempotent after reset)
    runBlocking { authService.bootstrapAdmin(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD) }

    configureAuth(jwtConfig)

    routing {
        route("/api/v1") {
            authRoutes(authService)
            projectRoutes(projectRepo, auditRepo, workspaceRepo)
            workspaceRoutes(workspaceRepo)
            userRoutes(userRepo)
            userManagementRoutes(authService, userRepo)
        }
    }
}
