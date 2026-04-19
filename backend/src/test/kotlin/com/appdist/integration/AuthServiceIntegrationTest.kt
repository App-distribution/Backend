package com.appdist.integration

import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.InvalidCredentialsException
import com.appdist.infrastructure.database.repository.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Service-level integration tests for AuthService using a real PostgreSQL container.
 * Containers are shared via [IntegrationContainers].
 */
class AuthServiceIntegrationTest {

    companion object {
        private val jwtConfig = AppConfig.JwtConfig(
            secret = "auth-service-integration-test-secret-32ch",
            issuer = "appdist",
            audience = "appdist-client",
            accessTokenTtlMinutes = 60L,
            refreshTokenTtlDays = 30L,
        )

        init {
            IntegrationContainers.initDatabase()
        }
    }

    private lateinit var authService: AuthService

    @BeforeTest
    fun setup() {
        authService = AuthService(
            userRepository = UserRepositoryImpl(),
            workspaceRepository = WorkspaceRepositoryImpl(),
            refreshTokenRepository = RefreshTokenRepositoryImpl(),
            jwtConfig = jwtConfig,
            auditRepository = AuditRepositoryImpl(),
        )
    }

    @Test
    fun `bootstrapAdmin creates admin user`() = runTest {
        val email = "bootstrap-${System.currentTimeMillis()}@corp-auth.com"
        authService.bootstrapAdmin(email, "securepassword")

        val user = UserRepositoryImpl().findByEmail(email)
        assertNotNull(user)
        assertEquals(UserRole.ADMIN, user.role)
        assertNotNull(user.workspaceId)
    }

    @Test
    fun `login returns tokens for valid credentials`() = runTest {
        val email = "login-${System.currentTimeMillis()}@corp-auth.com"
        authService.bootstrapAdmin(email, "correctpassword")

        val tokens = authService.login(email, "correctpassword")
        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)
        assertTrue(tokens.accessToken.isNotBlank())
        assertTrue(tokens.refreshToken.isNotBlank())
    }

    @Test
    fun `login throws InvalidCredentialsException for wrong password`() = runTest {
        val email = "wrong-pass-${System.currentTimeMillis()}@corp-auth.com"
        authService.bootstrapAdmin(email, "rightpassword")

        assertFailsWith<InvalidCredentialsException> {
            authService.login(email, "wrongpassword")
        }
    }

    @Test
    fun `login throws InvalidCredentialsException for unknown email`() = runTest {
        assertFailsWith<InvalidCredentialsException> {
            authService.login("nobody-${System.currentTimeMillis()}@corp-auth.com", "anypassword")
        }
    }

    @Test
    fun `refreshToken returns new tokens and revokes old`() = runTest {
        val email = "refresh-${System.currentTimeMillis()}@corp-auth.com"
        authService.bootstrapAdmin(email, "mypassword")
        val tokens = authService.login(email, "mypassword")

        val newTokens = authService.refreshToken(tokens.refreshToken)
        assertNotNull(newTokens.accessToken)
        assertNotNull(newTokens.refreshToken)
        assertTrue(newTokens.accessToken.isNotBlank())

        // Old refresh token should now be invalid
        val exception = assertFailsWith<IllegalStateException> {
            authService.refreshToken(tokens.refreshToken)
        }
        assertTrue(
            exception.message?.contains("Invalid") == true || exception.message?.contains("expired") == true,
            "Old refresh token should be revoked, got: ${exception.message}"
        )
    }

    @Test
    fun `bootstrapAdmin creates workspace with domain slug for corporate email`() = runTest {
        val ts = System.currentTimeMillis()
        val email = "founder@newcorp-$ts.com"
        authService.bootstrapAdmin(email, "adminpass")

        val user = UserRepositoryImpl().findByEmail(email)
        assertNotNull(user)
        val workspace = WorkspaceRepositoryImpl().findById(user.workspaceId!!)
        assertNotNull(workspace)
        assertTrue(workspace.slug.contains("newcorp-$ts"), "Expected slug to contain domain, got: ${workspace.slug}")
    }
}
