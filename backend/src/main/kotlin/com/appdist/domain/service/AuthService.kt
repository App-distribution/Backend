package com.appdist.domain.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import java.security.SecureRandom
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger {}

private const val BCRYPT_COST = 12
private const val PASSWORD_LENGTH = 12
private val PASSWORD_CHARSET = ('A'..'Z') + ('a'..'z') + ('0'..'9')

data class AuthTokens(val accessToken: String, val refreshToken: String)

open class AuthService(
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtConfig: AppConfig.JwtConfig,
    private val auditRepository: AuditRepository? = null,
) : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    private val secureRandom = SecureRandom()

    suspend fun login(email: String, password: String): AuthTokens {
        require(email.isNotBlank() && password.isNotBlank()) { "Email and password required" }
        val user = userRepository.findByEmailWithHash(email)
            ?: throw InvalidCredentialsException()
        val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
        if (!verified) throw InvalidCredentialsException()
        val tokens = issueTokens(user.user)
        launch {
            runCatching {
                auditRepository?.log(user.user.id, "user.login", "user", user.user.id)
            }.onFailure { log.warn(it) { "Audit log failed for user.login" } }
        }
        return tokens
    }

    suspend fun createUser(
        adminWorkspaceId: UUID,
        email: String,
        name: String,
        role: UserRole,
    ): Pair<com.appdist.domain.model.User, String> {
        require(role != UserRole.ADMIN) { "Cannot create ADMIN via this endpoint" }
        val plainPassword = generatePassword()
        val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray())
        val user = userRepository.create(adminWorkspaceId, email, name, role, hash)
        return user to plainPassword
    }

    suspend fun resetPassword(userId: UUID): String {
        val plainPassword = generatePassword()
        val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray())
        userRepository.updatePasswordHash(userId, hash)
        return plainPassword
    }

    suspend fun refreshToken(token: String): AuthTokens {
        val refreshToken = refreshTokenRepository.findValid(token)
            ?: error("Invalid or expired refresh token")
        refreshTokenRepository.revoke(token)
        val user = userRepository.findById(refreshToken.userId)
            ?: error("User not found")
        return issueTokens(user)
    }

    suspend fun bootstrapAdmin(email: String, password: String) {
        if (userRepository.findByEmail(email) != null) return
        val domain = email.substringAfter("@").lowercase()
        val slug = if (domain in FREE_EMAIL_DOMAINS) email.replace("@", "-at-").lowercase() else domain
        val workspace = workspaceRepository.findBySlug(slug)
            ?: workspaceRepository.create(name = slug, slug = slug)
        val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
        val admin = userRepository.create(workspace.id, email, email.substringBefore("@"), UserRole.ADMIN, hash)
        workspaceRepository.updateOwnerId(workspace.id, admin.id)
        log.info { "Bootstrap admin created: $email" }
    }

    private fun generatePassword(): String = (1..PASSWORD_LENGTH)
        .map { PASSWORD_CHARSET[secureRandom.nextInt(PASSWORD_CHARSET.size)] }
        .joinToString("")

    private suspend fun issueTokens(user: com.appdist.domain.model.User): AuthTokens {
        requireNotNull(user.workspaceId) { "Cannot issue token for user ${user.id} with no workspace" }
        val algorithm = Algorithm.HMAC256(jwtConfig.secret)
        val accessToken = JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withAudience(jwtConfig.audience)
            .withSubject(user.id.toString())
            .withClaim("email", user.email)
            .withClaim("role", user.role.name)
            .withClaim("workspace_id", user.workspaceId.toString())
            .withExpiresAt(Date.from(
                (Clock.System.now() + jwtConfig.accessTokenTtlMinutes.minutes).toJavaInstant()
            ))
            .sign(algorithm)
        val rawRefreshToken = UUID.randomUUID().toString()
        refreshTokenRepository.create(user.id, rawRefreshToken, jwtConfig.refreshTokenTtlDays)
        return AuthTokens(accessToken, rawRefreshToken)
    }
}

class InvalidCredentialsException : Exception("Invalid credentials")

val FREE_EMAIL_DOMAINS = setOf(
    "gmail.com", "googlemail.com", "yahoo.com", "yahoo.co.uk", "yahoo.co.in",
    "hotmail.com", "hotmail.co.uk", "outlook.com", "live.com", "msn.com",
    "icloud.com", "me.com", "mac.com", "protonmail.com", "proton.me",
    "aol.com", "yandex.com", "yandex.ru", "mail.ru", "inbox.com",
)
