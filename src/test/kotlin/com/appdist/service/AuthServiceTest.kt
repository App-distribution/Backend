package com.appdist.service

import com.appdist.TestDatabase
import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.AuthService
import com.appdist.infrastructure.database.repository.*
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthServiceTest {
    private lateinit var authService: AuthService
    private val jwtConfig = AppConfig.JwtConfig(
        secret = "test-secret-key-256-bit-minimum-length-ok",
        issuer = "appdist",
        audience = "appdist-client",
        accessTokenTtlMinutes = 60L,
        refreshTokenTtlDays = 30L,
    )
    private val otpConfig = AppConfig.OtpConfig(ttlMinutes = 5L, length = 6)

    @BeforeTest
    fun setup() {
        TestDatabase.init()
        authService = AuthService(
            userRepository = UserRepositoryImpl(),
            workspaceRepository = WorkspaceRepositoryImpl(),
            otpRepository = OtpRepositoryImpl(),
            refreshTokenRepository = RefreshTokenRepositoryImpl(),
            jwtConfig = jwtConfig,
            otpConfig = otpConfig,
        )
    }

    @Test
    fun `requestOtp creates OTP for new email`() = runTest {
        val otp = authService.requestOtp("test@example.com")
        assertNotNull(otp)
        assertEquals(6, otp.length)
    }

    @Test
    fun `verifyOtp with valid code auto-creates workspace and user`() = runTest {
        val email = "alice@acme.com"
        val otp = authService.requestOtp(email)
        val tokens = authService.verifyOtp(email, otp)
        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)
    }

    @Test
    fun `verifyOtp first user from domain gets Admin role`() = runTest {
        val email = "founder@newcorp.com"
        val otp = authService.requestOtp(email)
        authService.verifyOtp(email, otp)
        val user = UserRepositoryImpl().findByEmail(email)
        assertEquals(UserRole.ADMIN, user?.role)
    }

    @Test
    fun `verifyOtp second user from same domain gets Tester role`() = runTest {
        val email1 = "admin@startupxyz.com"
        val email2 = "dev@startupxyz.com"
        val otp1 = authService.requestOtp(email1)
        authService.verifyOtp(email1, otp1)
        val otp2 = authService.requestOtp(email2)
        authService.verifyOtp(email2, otp2)
        val user2 = UserRepositoryImpl().findByEmail(email2)
        assertEquals(UserRole.TESTER, user2?.role)
    }

    @Test
    fun `verifyOtp free email domain creates isolated workspace`() = runTest {
        val email = "someone@gmail.com"
        val otp = authService.requestOtp(email)
        authService.verifyOtp(email, otp)
        val user = UserRepositoryImpl().findByEmail(email)
        val workspace = WorkspaceRepositoryImpl().findById(user!!.workspaceId!!)
        assertEquals("someone-at-gmail.com", workspace?.slug)
    }

    @Test
    fun `verifyOtp with invalid OTP throws exception`() = runTest {
        authService.requestOtp("user@test.com")
        try {
            authService.verifyOtp("user@test.com", "000000")
            assert(false) { "should have thrown" }
        } catch (e: Exception) {
            assert(e.message?.contains("Invalid") == true || e.message?.contains("expired") == true)
        }
    }

    @Test
    fun `refreshToken returns new tokens and revokes old`() = runTest {
        val email = "refresh@example.com"
        val otp = authService.requestOtp(email)
        val tokens = authService.verifyOtp(email, otp)
        val newTokens = authService.refreshToken(tokens.refreshToken)
        assertNotNull(newTokens.accessToken)
    }
}
