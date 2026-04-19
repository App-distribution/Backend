package com.appdist.service

import com.appdist.TestDatabase
import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.InvalidCredentialsException
import com.appdist.infrastructure.database.repository.*
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class AuthServiceTest {
    private lateinit var authService: AuthService
    private val jwtConfig = AppConfig.JwtConfig(
        secret = "test-secret-key-256-bit-minimum-length-ok",
        issuer = "appdist",
        audience = "appdist-client",
        accessTokenTtlMinutes = 60L,
        refreshTokenTtlDays = 30L,
    )

    @BeforeTest
    fun setup() {
        TestDatabase.init()
        TestDatabase.reset()
        authService = AuthService(
            userRepository = UserRepositoryImpl(),
            workspaceRepository = WorkspaceRepositoryImpl(),
            refreshTokenRepository = RefreshTokenRepositoryImpl(),
            jwtConfig = jwtConfig,
        )
    }

    @Test
    fun `bootstrapAdmin creates admin user with correct role`() = runTest {
        authService.bootstrapAdmin("admin@example.com", "securepassword123")
        val user = UserRepositoryImpl().findByEmail("admin@example.com")
        assertNotNull(user)
        assertEquals(UserRole.ADMIN, user.role)
    }

    @Test
    fun `bootstrapAdmin is idempotent - second call does not throw`() = runTest {
        authService.bootstrapAdmin("admin@example.com", "password1")
        authService.bootstrapAdmin("admin@example.com", "password2")
        val user = UserRepositoryImpl().findByEmail("admin@example.com")
        assertNotNull(user)
    }

    @Test
    fun `login returns tokens for valid credentials`() = runTest {
        authService.bootstrapAdmin("login@example.com", "correctpassword")
        val tokens = authService.login("login@example.com", "correctpassword")
        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)
        assert(tokens.accessToken.isNotBlank())
        assert(tokens.refreshToken.isNotBlank())
    }

    @Test
    fun `login throws InvalidCredentialsException for wrong password`() = runTest {
        authService.bootstrapAdmin("wrong@example.com", "rightpassword")
        assertFailsWith<InvalidCredentialsException> {
            authService.login("wrong@example.com", "wrongpassword")
        }
    }

    @Test
    fun `login throws InvalidCredentialsException for unknown email`() = runTest {
        assertFailsWith<InvalidCredentialsException> {
            authService.login("nobody@example.com", "anypassword")
        }
    }

    @Test
    fun `refreshToken returns new tokens and revokes old`() = runTest {
        authService.bootstrapAdmin("refresh@example.com", "mypassword")
        val tokens = authService.login("refresh@example.com", "mypassword")
        val newTokens = authService.refreshToken(tokens.refreshToken)
        assertNotNull(newTokens.accessToken)
        assertNotNull(newTokens.refreshToken)
    }

    @Test
    fun `bootstrapAdmin creates workspace for corporate domain`() = runTest {
        authService.bootstrapAdmin("user@mycorp.com", "adminpass")
        val user = UserRepositoryImpl().findByEmail("user@mycorp.com")
        assertNotNull(user)
        val workspace = WorkspaceRepositoryImpl().findById(user.workspaceId!!)
        assertNotNull(workspace)
        assertEquals("mycorp.com", workspace.slug)
    }
}
