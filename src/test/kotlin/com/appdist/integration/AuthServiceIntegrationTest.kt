package com.appdist.integration

import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.AuthService
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
        private val otpConfig = AppConfig.OtpConfig(ttlMinutes = 5L, length = 6)

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
            otpRepository = OtpRepositoryImpl(),
            refreshTokenRepository = RefreshTokenRepositoryImpl(),
            jwtConfig = jwtConfig,
            otpConfig = otpConfig,
            auditRepository = AuditRepositoryImpl(),
        )
    }

    @Test
    fun `sendOtp inserts OTP in DB and returns 6-digit code`() = runTest {
        val email = "otp-test-${System.currentTimeMillis()}@corp-auth.com"
        val otp = authService.requestOtp(email)

        assertNotNull(otp)
        assertEquals(6, otp.length)
        assertTrue(otp.all { it.isDigit() }, "OTP should be all digits: $otp")
    }

    @Test
    fun `verifyOtp creates workspace and user with Admin role for new domain`() = runTest {
        val email = "founder-${System.currentTimeMillis()}@newcorp-auth.com"

        val otp = authService.requestOtp(email)
        val tokens = authService.verifyOtp(email, otp)

        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)
        assertTrue(tokens.accessToken.isNotBlank())
        assertTrue(tokens.refreshToken.isNotBlank())

        val user = UserRepositoryImpl().findByEmail(email)
        assertNotNull(user)
        assertEquals(UserRole.ADMIN, user.role)
        assertNotNull(user.workspaceId)

        val workspace = WorkspaceRepositoryImpl().findById(user.workspaceId!!)
        assertNotNull(workspace)
    }

    @Test
    fun `verifyOtp second user from same domain gets Tester role`() = runTest {
        val domain = "samecorp-${System.currentTimeMillis()}.com"
        val email1 = "admin@$domain"
        val email2 = "dev@$domain"

        val otp1 = authService.requestOtp(email1)
        authService.verifyOtp(email1, otp1)

        val otp2 = authService.requestOtp(email2)
        authService.verifyOtp(email2, otp2)

        val user2 = UserRepositoryImpl().findByEmail(email2)
        assertNotNull(user2)
        assertEquals(UserRole.TESTER, user2.role)

        // Both users should share the same workspace
        val user1 = UserRepositoryImpl().findByEmail(email1)
        assertNotNull(user1)
        assertEquals(user1.workspaceId, user2.workspaceId)
    }

    @Test
    fun `verifyOtp with invalid OTP throws exception`() = runTest {
        val email = "invalid-otp-${System.currentTimeMillis()}@corp-auth.com"
        authService.requestOtp(email)

        val exception = assertFailsWith<IllegalStateException> {
            authService.verifyOtp(email, "000000")
        }
        assertTrue(
            exception.message?.contains("Invalid") == true || exception.message?.contains("expired") == true,
            "Expected invalid/expired OTP error, got: ${exception.message}"
        )
    }

    @Test
    fun `refreshToken returns new tokens and revokes old`() = runTest {
        val email = "refresh-${System.currentTimeMillis()}@corp-auth.com"

        val otp = authService.requestOtp(email)
        val tokens = authService.verifyOtp(email, otp)

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
    fun `verifyOtp with free email domain creates isolated workspace`() = runTest {
        val email = "someone-${System.currentTimeMillis()}@gmail.com"

        val otp = authService.requestOtp(email)
        authService.verifyOtp(email, otp)

        val user = UserRepositoryImpl().findByEmail(email)
        assertNotNull(user)

        val workspace = WorkspaceRepositoryImpl().findById(user.workspaceId!!)
        assertNotNull(workspace)
        // Free email domain → slug is derived from email, not domain
        assertTrue(
            workspace.slug.contains("gmail"),
            "Expected workspace slug to contain 'gmail' for free email domain, got: ${workspace.slug}"
        )
    }
}
